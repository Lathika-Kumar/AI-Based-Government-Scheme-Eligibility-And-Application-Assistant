package com.schemebridge.service.impl;

import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.dto.ProfileRequest;
import com.schemebridge.dto.ProfileResponse;
import com.schemebridge.dto.ProfileSummaryResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.ProfileAuditLog;
import com.schemebridge.entity.User;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.ProfileMapper;
import com.schemebridge.repository.CitizenProfileRepository;
import com.schemebridge.repository.ProfileAuditLogRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.CitizenProfileService;
import com.schemebridge.util.ProfileCompletionCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenProfileServiceImpl implements CitizenProfileService {

    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final ProfileAuditLogRepository profileAuditLogRepository;
    private final ProfileCompletionCalculator completionCalculator;
    private final ProfileMapper profileMapper;

    @Override
    public ProfileResponse getProfile(String userEmail) {
        log.info("Fetching profile for authenticated user: {}", userEmail);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());
        return profileMapper.toProfileResponse(user, profile);
    }

    @Override
    public ProfileResponse updateProfile(String userEmail, ProfileRequest request) {
        log.info("Updating profile for authenticated user: {}", userEmail);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());

        Set<String> updatedFields = new HashSet<>();

        // Full Name update on User (read-only email preserved)
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            String trimmedName = request.getFullName().trim();
            if (!trimmedName.equals(user.getFullName())) {
                user.setFullName(trimmedName);
                updatedFields.add("fullName");
            }
        }

        // Date of Birth & Age Validation
        if (request.getDateOfBirth() != null) {
            if (request.getDateOfBirth().isAfter(LocalDate.now())) {
                throw new BadRequestException("Date of birth cannot be a future date.");
            }
            profile.setDateOfBirth(request.getDateOfBirth());
            int calculatedAge = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();

            if (request.getAge() != null && Math.abs(request.getAge() - calculatedAge) > 1) {
                throw new BadRequestException("Age does not match provided date of birth.");
            }
            profile.setAge(calculatedAge);
            updatedFields.add("dateOfBirth");
            updatedFields.add("age");
        } else if (request.getAge() != null) {
            profile.setAge(request.getAge());
            updatedFields.add("age");
        }

        if (request.getGender() != null) {
            profile.setGender(request.getGender().trim());
            updatedFields.add("gender");
        }
        if (request.getMaritalStatus() != null) {
            profile.setMaritalStatus(request.getMaritalStatus().trim());
            updatedFields.add("maritalStatus");
        }
        if (request.getState() != null) {
            profile.setState(request.getState().trim());
            updatedFields.add("state");
        }
        if (request.getDistrict() != null) {
            profile.setDistrict(request.getDistrict().trim());
            updatedFields.add("district");
        }
        if (request.getCityOrVillage() != null) {
            profile.setCityOrVillage(request.getCityOrVillage().trim());
            updatedFields.add("cityOrVillage");
        }
        if (request.getPincode() != null) {
            profile.setPincode(request.getPincode().trim());
            updatedFields.add("pincode");
        }
        if (request.getAnnualIncome() != null) {
            profile.setAnnualIncome(request.getAnnualIncome());
            updatedFields.add("annualIncome");
        }
        if (request.getCategory() != null) {
            profile.setCategory(request.getCategory().trim());
            updatedFields.add("category");
        }
        if (request.getOccupation() != null) {
            profile.setOccupation(request.getOccupation().trim());
            updatedFields.add("occupation");
        }
        if (request.getEmploymentStatus() != null) {
            profile.setEmploymentStatus(request.getEmploymentStatus().trim());
            updatedFields.add("employmentStatus");
        }
        if (request.getDisabilityStatus() != null) {
            profile.setDisabilityStatus(request.getDisabilityStatus());
            updatedFields.add("disabilityStatus");
        }
        if (request.getQualification() != null) {
            profile.setQualification(request.getQualification().trim());
            updatedFields.add("qualification");
        }
        if (request.getAadhaarNumber() != null) {
            profile.setAadhaarNumber(request.getAadhaarNumber().replaceAll("\\D", ""));
            updatedFields.add("aadhaarNumber");
        }

        // PAN Number Uppercase Transformation
        if (request.getPanNumber() != null) {
            profile.setPanNumber(request.getPanNumber().trim().toUpperCase());
            updatedFields.add("panNumber");
        }

        // Profile Versioning & Timestamps
        int currentVersion = profile.getProfileVersion() != null ? profile.getProfileVersion() : 1;
        profile.setProfileVersion(currentVersion + 1);
        profile.setLastUpdatedAt(LocalDateTime.now());
        profile.setLastUpdatedBy(userEmail);

        // Check Onboarding Completion Transition
        boolean isMandatoryDone = completionCalculator.isMandatoryComplete(user, profile);
        if (isMandatoryDone && !Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            user.setOnboardingCompleted(true);
            if (profile.getProfileCompletedAt() == null) {
                profile.setProfileCompletedAt(LocalDateTime.now());
                log.info("Profile completed for the first time for user: {}", userEmail);
            }
        }

        userRepository.save(user);
        CitizenProfile savedProfile = citizenProfileRepository.save(profile);

        // Persist Audit Log
        if (!updatedFields.isEmpty()) {
            ProfileAuditLog auditLog = ProfileAuditLog.builder()
                    .userId(user.getId())
                    .updatedFields(updatedFields)
                    .updatedAt(LocalDateTime.now())
                    .updatedBy(userEmail)
                    .build();
            profileAuditLogRepository.save(auditLog);
            log.info("Profile audit log created for user {} with updated fields: {}", userEmail, updatedFields);
        }

        return profileMapper.toProfileResponse(user, savedProfile);
    }

    @Override
    public ProfileCompletionResponse getProfileCompletion(String userEmail) {
        log.info("Calculating profile completion score for user: {}", userEmail);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());
        return completionCalculator.calculateCompletion(user, profile);
    }

    @Override
    public ProfileSummaryResponse getProfileSummary(String userEmail) {
        log.info("Fetching lightweight dashboard summary for user: {}", userEmail);
        User user = getUserByEmail(userEmail);
        CitizenProfile profile = getOrCreateProfile(user.getId());
        ProfileCompletionResponse completion = completionCalculator.calculateCompletion(user, profile);

        return ProfileSummaryResponse.builder()
                .completionPercentage(completion.getCompletionPercentage())
                .status(completion.getStatus())
                .fullName(user.getFullName())
                .state(profile.getState())
                .district(profile.getDistrict())
                .onboardingCompleted(Boolean.TRUE.equals(user.getOnboardingCompleted()))
                .build();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private CitizenProfile getOrCreateProfile(String userId) {
        return citizenProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Initializing new CitizenProfile document for userId: {}", userId);
                    CitizenProfile newProfile = CitizenProfile.builder()
                            .userId(userId)
                            .profileVersion(1)
                            .deleted(false)
                            .disabilityStatus(false)
                            .build();
                    return citizenProfileRepository.save(newProfile);
                });
    }
}
