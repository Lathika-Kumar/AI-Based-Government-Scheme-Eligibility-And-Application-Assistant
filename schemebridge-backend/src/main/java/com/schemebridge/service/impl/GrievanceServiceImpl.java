package com.schemebridge.service.impl;

import com.schemebridge.dto.GrievanceRequest;
import com.schemebridge.dto.GrievanceResponse;
import com.schemebridge.entity.Grievance;
import com.schemebridge.entity.User;
import com.schemebridge.enums.GrievanceCategory;
import com.schemebridge.enums.GrievanceStatus;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.GrievanceMapper;
import com.schemebridge.repository.GrievanceRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceServiceImpl implements GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final GrievanceMapper grievanceMapper;

    @Override
    public GrievanceResponse createGrievance(String userEmail, GrievanceRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        GrievanceCategory category;
        try {
            category = GrievanceCategory.valueOf(request.getCategory().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid grievance category: " + request.getCategory());
        }

        Grievance grievance = Grievance.builder()
                .userId(user.getId())
                .subject(request.getSubject())
                .description(request.getDescription())
                .category(category)
                .status(GrievanceStatus.OPEN)
                .build();

        Grievance saved = grievanceRepository.save(grievance);
        log.info("Grievance {} created by user {}", saved.getId(), userEmail);
        return grievanceMapper.toResponse(saved);
    }

    @Override
    public List<GrievanceResponse> getGrievances(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return grievanceRepository.findByUserId(user.getId()).stream()
                .map(grievanceMapper::toResponse)
                .toList();
    }

    @Override
    public List<GrievanceResponse> getAllGrievancesForAdmin() {
        return grievanceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(grievanceMapper::toResponse)
                .toList();
    }

    @Override
    public GrievanceResponse resolveGrievance(String id, String adminEmail, String resolutionNotes) {
        Grievance grievance = grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with id: " + id));

        if (grievance.getStatus() == GrievanceStatus.RESOLVED) {
            throw new BadRequestException("Grievance is already resolved.");
        }

        grievance.setStatus(GrievanceStatus.RESOLVED);
        grievance.setResolutionNotes(resolutionNotes);
        grievance.setResolvedAt(Instant.now());
        Grievance saved = grievanceRepository.save(grievance);
        log.info("Grievance {} resolved by admin {}", id, adminEmail);
        return grievanceMapper.toResponse(saved);
    }
}
