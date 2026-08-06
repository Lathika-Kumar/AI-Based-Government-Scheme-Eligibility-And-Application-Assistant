package com.schemebridge.service.impl;

import com.schemebridge.dto.ApplicationRequest;
import com.schemebridge.dto.ApplicationResponse;
import com.schemebridge.dto.ApplicationTimelineEntryResponse;
import com.schemebridge.entity.Application;
import com.schemebridge.entity.SavedScheme;
import com.schemebridge.entity.User;
import com.schemebridge.enums.ApplicationStatus;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.ApplicationMapper;
import com.schemebridge.repository.ApplicationRepository;
import com.schemebridge.repository.SavedSchemeRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final SavedSchemeRepository savedSchemeRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    public List<ApplicationResponse> getApplications(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Application> applications = applicationRepository.findByUserId(user.getId());
        return applications.stream().map(applicationMapper::toApplicationResponse).collect(Collectors.toList());
    }

    @Override
    public ApplicationResponse getApplicationById(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        if (!application.getUserId().equals(user.getId())) {
            throw new BadRequestException("Application does not belong to the authenticated user.");
        }
        return applicationMapper.toApplicationResponse(application);
    }

    @Override
    public ApplicationResponse submitApplication(String userEmail, ApplicationRequest request) {
        User user = getUserByEmail(userEmail);
        if (request.getSchemeId() == null || request.getSchemeId().isBlank()) {
            throw new BadRequestException("Scheme ID is required to submit an application.");
        }

        Optional<Application> existingApplication = applicationRepository.findByUserIdAndSchemeId(user.getId(), request.getSchemeId());
        if (existingApplication.isPresent() && existingApplication.get().getStatus() != ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("Duplicate application submission is not allowed for the same scheme.");
        }

        Application application = applicationMapper.toEntity(request, user.getId());
        application.setReferenceNumber(generateUniqueReferenceNumber(application.getReferenceNumber()));
        application.setTimeline(buildTimeline(ApplicationStatus.SUBMITTED, "Application submitted successfully."));
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(Instant.now());
        application.setCreatedAt(Instant.now());
        application.setUpdatedAt(Instant.now());

        Application saved = applicationRepository.save(application);
        log.info("Application {} submitted for user {}", saved.getId(), userEmail);
        return applicationMapper.toApplicationResponse(saved);
    }

    @Override
    public List<ApplicationTimelineEntryResponse> getApplicationTimeline(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        if (!application.getUserId().equals(user.getId())) {
            throw new BadRequestException("Application does not belong to the authenticated user.");
        }
        return applicationMapper.toTimelineResponse(application.getTimeline());
    }

    @Override
    public void withdrawApplication(String id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        if (!application.getUserId().equals(user.getId())) {
            throw new BadRequestException("Application does not belong to the authenticated user.");
        }
        if (application.getStatus() == ApplicationStatus.APPROVED || application.getStatus() == ApplicationStatus.DISBURSED) {
            throw new BadRequestException("Approved or disbursed applications cannot be withdrawn.");
        }
        if (application.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BadRequestException("Application is already withdrawn.");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        application.getTimeline().addAll(buildTimeline(ApplicationStatus.WITHDRAWN, "Application withdrawn by the citizen."));
        applicationRepository.save(application);
        log.info("Application {} withdrawn by user {}", id, userEmail);
    }

    @Override
    public List<String> getSavedSchemes(String userEmail) {
        User user = getUserByEmail(userEmail);
        return savedSchemeRepository.findByUserId(user.getId()).stream()
                .map(SavedScheme::getSchemeId)
                .collect(Collectors.toList());
    }

    @Override
    public void saveScheme(String userEmail, String schemeId) {
        User user = getUserByEmail(userEmail);
        if (savedSchemeRepository.findByUserIdAndSchemeId(user.getId(), schemeId).isPresent()) {
            return;
        }
        SavedScheme savedScheme = SavedScheme.builder()
                .userId(user.getId())
                .schemeId(schemeId)
                .savedAt(Instant.now())
                .build();
        savedSchemeRepository.save(savedScheme);
        log.info("Scheme {} saved for user {}", schemeId, userEmail);
    }

    @Override
    public void unsaveScheme(String userEmail, String schemeId) {
        User user = getUserByEmail(userEmail);
        savedSchemeRepository.deleteByUserIdAndSchemeId(user.getId(), schemeId);
        log.info("Scheme {} unsaved for user {}", schemeId, userEmail);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private String generateUniqueReferenceNumber(String seedReferenceNumber) {
        String candidate = seedReferenceNumber;
        int attempt = 0;
        while (applicationRepository.findByReferenceNumber(candidate).isPresent() && attempt < 10) {
            candidate = seedReferenceNumber + "-" + attempt;
            attempt++;
        }
        if (applicationRepository.findByReferenceNumber(candidate).isPresent()) {
            throw new BadRequestException("Unable to generate a unique tracking reference for the application.");
        }
        return candidate;
    }

    private List<com.schemebridge.entity.ApplicationTimelineEntry> buildTimeline(ApplicationStatus stage, String notes) {
        List<com.schemebridge.entity.ApplicationTimelineEntry> timeline = new ArrayList<>();
        timeline.add(com.schemebridge.entity.ApplicationTimelineEntry.builder()
                .stage(stage)
                .completedAt(Instant.now())
                .notes(notes)
                .build());
        return timeline;
    }
}
