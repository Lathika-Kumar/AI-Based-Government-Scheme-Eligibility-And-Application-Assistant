package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.VerifiedAttribute;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CitizenProfileRequest;
import com.schemebridge.scheme.dto.response.CitizenProfileResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;

/**
 * Service for citizen_profiles collection.
 *
 * Security contract:
 *   - All operations require userId derived from verified JWT authentication.
 *   - Identity remains in Oracle USERS; demographic eligibility and document verification
 *     provenance are managed here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenProfileService {

    private final CitizenProfileRepository citizenProfileRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    public Optional<CitizenProfile> findProfile(String userId) {
        return citizenProfileRepository.findByUserId(userId);
    }

    public CitizenProfile getOrCreateProfile(String userId) {
        return citizenProfileRepository.findByUserId(userId)
                .map(profile -> {
                    if (profile.getOnboardingStatus() == null || profile.getOnboardingStep() == null || profile.getOnboardingStep() == 0) {
                        calculateAndSetOnboardingState(profile);
                        CitizenProfile saved = citizenProfileRepository.save(profile);
                        log.info("Normalized legacy citizen_profile for userId={}, onboardingStatus={}, onboardingStep={}, onboardingComplete={}",
                                userId, saved.getOnboardingStatus(), saved.getOnboardingStep(), saved.getOnboardingComplete());
                        return saved;
                    }
                    return profile;
                })
                .orElseGet(() -> {
                    CitizenProfile empty = CitizenProfile.builder()
                            .userId(userId)
                            .onboardingComplete(false)
                            .onboardingStatus("NOT_STARTED")
                            .onboardingStep(1)
                            .verifiedAttributes(new HashMap<>())
                            .accessibilityPreferences(new HashMap<>())
                            .build();
                    CitizenProfile saved = citizenProfileRepository.save(empty);
                    log.info("Created empty citizen profile for userId={}", userId);
                    return saved;
                });
    }

    public boolean isOnboardingComplete(String userId) {
        return citizenProfileRepository.findByUserId(userId)
                .map(p -> Boolean.TRUE.equals(p.getOnboardingComplete()) || "COMPLETE".equalsIgnoreCase(p.getOnboardingStatus()))
                .orElse(false);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public CitizenProfile upsertProfile(String userId, CitizenProfileRequest request) {
        CitizenProfile profile = citizenProfileRepository.findByUserId(userId)
                .orElseGet(() -> CitizenProfile.builder()
                        .userId(userId)
                        .onboardingComplete(false)
                        .onboardingStatus("NOT_STARTED")
                        .onboardingStep(1)
                        .verifiedAttributes(new HashMap<>())
                        .accessibilityPreferences(new HashMap<>())
                        .build());

        // Apply partial updates with null-safety and dob/age synchronization
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            profile.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDob() != null) {
            profile.setDob(request.getDob());
            int calculatedAge = java.time.Period.between(request.getDob(), java.time.LocalDate.now()).getYears();
            profile.setAge(calculatedAge);
        } else if (request.getAge() != null && request.getAge() > 0) {
            profile.setAge(request.getAge());
            if (profile.getDob() == null) {
                profile.setDob(java.time.LocalDate.now().minusYears(request.getAge()));
            }
        }
        if (request.getGender()               != null && !request.getGender().isBlank()) profile.setGender(request.getGender());
        if (request.getMaritalStatus()        != null && !request.getMaritalStatus().isBlank()) profile.setMaritalStatus(request.getMaritalStatus());
        if (request.getState()                != null && !request.getState().isBlank()) profile.setState(request.getState());
        if (request.getDistrict()             != null && !request.getDistrict().isBlank()) profile.setDistrict(request.getDistrict());
        if (request.getPincode()              != null && !request.getPincode().isBlank()) profile.setPincode(request.getPincode());
        if (request.getResidentialAreaType()  != null && !request.getResidentialAreaType().isBlank()) profile.setResidentialAreaType(request.getResidentialAreaType());
        if (request.getAnnualIncome()         != null) profile.setAnnualIncome(request.getAnnualIncome());
        if (request.getBplStatus()            != null) profile.setBplStatus(request.getBplStatus());
        if (request.getRationCardType()       != null && !request.getRationCardType().isBlank()) profile.setRationCardType(request.getRationCardType());
        if (request.getOccupation()           != null && !request.getOccupation().isBlank()) profile.setOccupation(request.getOccupation());
        if (request.getEmploymentStatus()     != null && !request.getEmploymentStatus().isBlank()) profile.setEmploymentStatus(request.getEmploymentStatus());
        if (request.getIsFarmer()             != null) profile.setIsFarmer(request.getIsFarmer());
        if (request.getLandholdingArea()      != null) profile.setLandholdingArea(request.getLandholdingArea());
        if (request.getIsStudent()            != null) profile.setIsStudent(request.getIsStudent());
        if (request.getSocialCategory()       != null && !request.getSocialCategory().isBlank()) profile.setSocialCategory(request.getSocialCategory());
        if (request.getMinorityStatus()       != null) profile.setMinorityStatus(request.getMinorityStatus());
        if (request.getEducation()            != null && !request.getEducation().isBlank()) profile.setEducation(request.getEducation());
        if (request.getDisabilityStatus()     != null) profile.setDisabilityStatus(request.getDisabilityStatus());
        if (request.getDisabilityType()       != null && !request.getDisabilityType().isBlank()) profile.setDisabilityType(request.getDisabilityType());
        if (request.getDisabilityPercentage() != null) profile.setDisabilityPercentage(request.getDisabilityPercentage());
        if (request.getUdidNumber()           != null && !request.getUdidNumber().isBlank()) profile.setUdidNumber(request.getUdidNumber());

        if (request.getAccessibilityPreferences() != null) {
            profile.setAccessibilityPreferences(request.getAccessibilityPreferences());
        }

        // Server-authoritative onboarding lifecycle calculation
        calculateAndSetOnboardingState(profile);

        CitizenProfile saved = citizenProfileRepository.save(profile);
        log.info("Upserted citizen_profile for userId={}, name={}, age={}, onboardingStatus={}, onboardingStep={}, onboardingComplete={}",
                userId, saved.getDisplayName(), saved.getAge(), saved.getOnboardingStatus(), saved.getOnboardingStep(), saved.getOnboardingComplete());
        return saved;
    }

    /**
     * Links a verified document to an eligibility attribute with provenance metadata.
     */
    public CitizenProfile linkVerifiedDocumentAttribute(String userId, String attributeKey, Object value,
                                                         String source, String documentId, String documentCode,
                                                         Double confidenceScore) {
        CitizenProfile profile = getOrCreateProfile(userId);
        if (profile.getVerifiedAttributes() == null) {
            profile.setVerifiedAttributes(new HashMap<>());
        }

        VerifiedAttribute<Object> attr = VerifiedAttribute.builder()
                .value(value)
                .source(source != null ? source : "DOCUMENT_VERIFIED")
                .verified(true)
                .documentId(documentId)
                .documentCode(documentCode)
                .confidenceScore(confidenceScore != null ? confidenceScore : 1.0)
                .verifiedAt(Instant.now())
                .build();

        profile.getVerifiedAttributes().put(attributeKey, attr);

        // Update corresponding scalar field
        switch (attributeKey) {
            case "annualIncome" -> { if (value instanceof Number n) profile.setAnnualIncome(n.doubleValue()); }
            case "socialCategory" -> profile.setSocialCategory(String.valueOf(value));
            case "state" -> profile.setState(String.valueOf(value));
            case "disabilityStatus" -> profile.setDisabilityStatus(Boolean.parseBoolean(String.valueOf(value)));
            case "landholdingArea" -> { if (value instanceof Number n) profile.setLandholdingArea(n.doubleValue()); }
            case "dob" -> { if (value instanceof String s) { try { profile.setDob(java.time.LocalDate.parse(s)); } catch (Exception e) {} } }
        }

        return citizenProfileRepository.save(profile);
    }

    private void calculateAndSetOnboardingState(CitizenProfile profile) {
        // Step 1: Personal demographics
        boolean step1Valid = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                && ((profile.getAge() != null && profile.getAge() > 0) || profile.getDob() != null)
                && profile.getGender() != null && !profile.getGender().isBlank();

        // Step 2: Socio-economic / Eligibility
        boolean step2Valid = step1Valid
                && profile.getOccupation() != null && !profile.getOccupation().isBlank()
                && profile.getAnnualIncome() != null && profile.getAnnualIncome() >= 0
                && profile.getSocialCategory() != null && !profile.getSocialCategory().isBlank()
                && profile.getState() != null && !profile.getState().isBlank();

        // Step 3: Supporting / Disability & Finalize
        boolean step3Valid = step2Valid
                && profile.getDisabilityStatus() != null;

        if (step3Valid) {
            profile.setOnboardingComplete(true);
            profile.setOnboardingStatus("COMPLETE");
            profile.setOnboardingStep(3);
        } else if (step2Valid) {
            profile.setOnboardingComplete(false);
            profile.setOnboardingStatus("IN_PROGRESS");
            profile.setOnboardingStep(3);
        } else if (step1Valid) {
            profile.setOnboardingComplete(false);
            profile.setOnboardingStatus("IN_PROGRESS");
            profile.setOnboardingStep(2);
        } else {
            boolean hasAnyData = (profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                    || profile.getAge() != null
                    || (profile.getGender() != null && !profile.getGender().isBlank());
            profile.setOnboardingComplete(false);
            profile.setOnboardingStatus(hasAnyData ? "IN_PROGRESS" : "NOT_STARTED");
            profile.setOnboardingStep(1);
        }
    }

    // ── Conversion ────────────────────────────────────────────────────────────

    public CitizenEligibilityProfile toCitizenEligibilityProfile(CitizenProfile profile) {
        if (profile == null) return null;

        Integer resolvedAge = profile.getAge();
        if ((resolvedAge == null || resolvedAge <= 0) && profile.getDob() != null) {
            resolvedAge = java.time.Period.between(profile.getDob(), java.time.LocalDate.now()).getYears();
        }

        Boolean isFarmer = profile.getIsFarmer();
        if (isFarmer == null && profile.getOccupation() != null) {
            String occLower = profile.getOccupation().toLowerCase();
            if (occLower.contains("farmer") || occLower.contains("agriculture") || occLower.contains("kisan")) {
                isFarmer = true;
            }
        }

        Boolean isStudent = profile.getIsStudent();
        if (isStudent == null && profile.getOccupation() != null) {
            String occLower = profile.getOccupation().toLowerCase();
            if (occLower.contains("student") || occLower.contains("scholar")) {
                isStudent = true;
            }
        }

        CitizenEligibilityProfile dto = CitizenEligibilityProfile.builder()
                .age(resolvedAge)
                .gender(profile.getGender())
                .annualIncome(profile.getAnnualIncome())
                .occupation(profile.getOccupation())
                .state(profile.getState())
                .district(profile.getDistrict())
                .socialCategory(profile.getSocialCategory())
                .disabilityStatus(profile.getDisabilityStatus())
                .isFarmer(isFarmer)
                .isStudent(isStudent)
                .bplStatus(profile.getBplStatus())
                .maritalStatus(profile.getMaritalStatus())
                .employmentStatus(profile.getEmploymentStatus())
                .residentialAreaType(profile.getResidentialAreaType())
                .rationCardType(profile.getRationCardType())
                .landholdingArea(profile.getLandholdingArea())
                .minorityStatus(profile.getMinorityStatus())
                .build();

        if (profile.getDistrict() != null) dto.getAttributes().put("district", profile.getDistrict());
        if (profile.getDob() != null) dto.getAttributes().put("dob", profile.getDob().toString());
        if (profile.getMaritalStatus() != null) dto.getAttributes().put("maritalStatus", profile.getMaritalStatus());
        if (profile.getPincode() != null) dto.getAttributes().put("pincode", profile.getPincode());
        if (profile.getResidentialAreaType() != null) dto.getAttributes().put("residentialAreaType", profile.getResidentialAreaType());
        if (profile.getRationCardType() != null) dto.getAttributes().put("rationCardType", profile.getRationCardType());
        if (profile.getEmploymentStatus() != null) dto.getAttributes().put("employmentStatus", profile.getEmploymentStatus());
        if (profile.getEducation() != null) dto.getAttributes().put("education", profile.getEducation());

        if (profile.getVerifiedAttributes() != null) {
            dto.getAttributes().put("verifiedAttributes", profile.getVerifiedAttributes());
        }
        if (isFarmer != null) dto.getAttributes().put("isFarmer", isFarmer);
        if (profile.getLandholdingArea() != null) dto.getAttributes().put("landholdingArea", profile.getLandholdingArea());
        if (isStudent != null) dto.getAttributes().put("isStudent", isStudent);
        if (profile.getMinorityStatus() != null) dto.getAttributes().put("minorityStatus", profile.getMinorityStatus());
        if (profile.getBplStatus() != null) dto.getAttributes().put("bplStatus", profile.getBplStatus());

        return dto;
    }

    public CitizenProfile toCitizenProfile(CitizenEligibilityProfile dto, String userId) {
        if (dto == null) return CitizenProfile.builder().userId(userId != null ? userId : "citizen").build();
        return CitizenProfile.builder()
                .userId(userId != null ? userId : "citizen")
                .age(dto.getAge())
                .gender(dto.getGender())
                .annualIncome(dto.getAnnualIncome())
                .occupation(dto.getOccupation())
                .state(dto.getState())
                .socialCategory(dto.getSocialCategory())
                .disabilityStatus(dto.getDisabilityStatus())
                .isFarmer(dto.getIsFarmer())
                .isStudent(dto.getIsStudent())
                .bplStatus(dto.getBplStatus())
                .maritalStatus(dto.getMaritalStatus())
                .district(dto.getDistrict())
                .employmentStatus(dto.getEmploymentStatus())
                .residentialAreaType(dto.getResidentialAreaType())
                .rationCardType(dto.getRationCardType())
                .landholdingArea(dto.getLandholdingArea())
                .minorityStatus(dto.getMinorityStatus())
                .build();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    public CitizenProfileResponse toResponse(CitizenProfile profile) {
        boolean complete = Boolean.TRUE.equals(profile.getOnboardingComplete()) || "COMPLETE".equalsIgnoreCase(profile.getOnboardingStatus());
        String status = profile.getOnboardingStatus() != null ? profile.getOnboardingStatus() : (complete ? "COMPLETE" : "NOT_STARTED");
        int step = profile.getOnboardingStep() != null ? profile.getOnboardingStep() : (complete ? 3 : 1);

        Integer resolvedAge = profile.getAge();
        if ((resolvedAge == null || resolvedAge <= 0) && profile.getDob() != null) {
            resolvedAge = java.time.Period.between(profile.getDob(), java.time.LocalDate.now()).getYears();
        }

        return CitizenProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .displayName(profile.getDisplayName())
                .dob(profile.getDob())
                .age(resolvedAge)
                .gender(profile.getGender())
                .maritalStatus(profile.getMaritalStatus())
                .state(profile.getState())
                .district(profile.getDistrict())
                .pincode(profile.getPincode())
                .residentialAreaType(profile.getResidentialAreaType())
                .annualIncome(profile.getAnnualIncome())
                .bplStatus(profile.getBplStatus())
                .rationCardType(profile.getRationCardType())
                .occupation(profile.getOccupation())
                .employmentStatus(profile.getEmploymentStatus())
                .isFarmer(profile.getIsFarmer())
                .landholdingArea(profile.getLandholdingArea())
                .isStudent(profile.getIsStudent())
                .socialCategory(profile.getSocialCategory())
                .minorityStatus(profile.getMinorityStatus())
                .education(profile.getEducation())
                .disabilityStatus(profile.getDisabilityStatus())
                .disabilityType(profile.getDisabilityType())
                .disabilityPercentage(profile.getDisabilityPercentage())
                .udidNumber(profile.getUdidNumber())
                .verifiedAttributes(profile.getVerifiedAttributes())
                .onboardingComplete(complete)
                .onboardingStatus(status)
                .onboardingStep(step)
                .accessibilityPreferences(profile.getAccessibilityPreferences())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
