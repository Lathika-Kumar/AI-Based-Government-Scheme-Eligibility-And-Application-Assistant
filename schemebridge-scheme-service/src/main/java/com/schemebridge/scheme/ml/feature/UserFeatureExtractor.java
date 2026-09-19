package com.schemebridge.scheme.ml.feature;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import org.springframework.stereotype.Component;

/**
 * Extracts privacy-safe UserFeatures from CitizenProfile or CitizenEligibilityProfile.
 * Gracefully handles nulls, missing attributes, and cold-start citizens.
 * Zero direct PII is extracted.
 */
@Component
public class UserFeatureExtractor {

    public UserFeatures extractFromCitizenProfile(CitizenProfile profile) {
        if (profile == null) {
            return buildEmptyUserFeatures();
        }

        return UserFeatures.builder()
                .ageBucket(determineAgeBucket(profile.getAge()))
                .stateCode(normalizeString(profile.getState()))
                .district(normalizeString(profile.getDistrict()))
                .occupationCode(normalizeString(profile.getOccupation()))
                .incomeTier(determineIncomeTier(profile.getAnnualIncome(), profile.getBplStatus()))
                .categoryCode(normalizeString(profile.getSocialCategory()))
                .genderCode(normalizeString(profile.getGender()))
                .disabilityStatus(profile.getDisabilityStatus())
                .isFarmer(profile.getIsFarmer())
                .isStudent(profile.getIsStudent())
                .bplStatus(profile.getBplStatus())
                .educationLevel(normalizeString(profile.getEducation()))
                .build();
    }

    public UserFeatures extractFromEligibilityProfile(CitizenEligibilityProfile profile) {
        if (profile == null) {
            return buildEmptyUserFeatures();
        }

        Boolean isFarmer = profile.getIsFarmer();
        Boolean isStudent = profile.getIsStudent();
        Boolean bplCardHolder = profile.getBplStatus();

        if (profile.getAttributes() != null) {
            if (isStudent == null) {
                Object studentObj = profile.getAttributes().get("isStudent");
                if (studentObj instanceof Boolean b) isStudent = b;
            }

            if (bplCardHolder == null) {
                Object bplObj = profile.getAttributes().get("bplStatus");
                if (bplObj == null) bplObj = profile.getAttributes().get("bplCardHolder");
                if (bplObj instanceof Boolean b) bplCardHolder = b;
            }

            if (isFarmer == null) {
                Object farmerObj = profile.getAttributes().get("isFarmer");
                if (farmerObj instanceof Boolean b) isFarmer = b;
            }
        }

        String occ = profile.getOccupation();
        if (isFarmer == null && occ != null && occ.toUpperCase().contains("FARMER")) {
            isFarmer = true;
        }
        if (isStudent == null && occ != null && occ.toUpperCase().contains("STUDENT")) {
            isStudent = true;
        }

        Double income = profile.getAnnualIncome() != null ? profile.getAnnualIncome().doubleValue() : null;

        String dist = profile.getDistrict();
        if (dist == null && profile.getAttributes() != null && profile.getAttributes().get("district") != null) {
            dist = profile.getAttributes().get("district").toString();
        }

        String edu = "UNKNOWN";
        if (profile.getAttributes() != null && profile.getAttributes().get("education") != null) {
            edu = normalizeString(profile.getAttributes().get("education").toString());
        }

        return UserFeatures.builder()
                .ageBucket(determineAgeBucket(profile.getAge()))
                .stateCode(normalizeString(profile.getState()))
                .district(dist != null ? normalizeString(dist) : "UNKNOWN")
                .occupationCode(normalizeString(profile.getOccupation()))
                .incomeTier(determineIncomeTier(income, bplCardHolder))
                .categoryCode(normalizeString(profile.getSocialCategory()))
                .genderCode(normalizeString(profile.getGender()))
                .disabilityStatus(profile.getDisabilityStatus())
                .isFarmer(isFarmer)
                .isStudent(isStudent)
                .bplStatus(bplCardHolder)
                .educationLevel(edu)
                .build();
    }

    public UserFeatures buildEmptyUserFeatures() {
        return UserFeatures.builder()
                .ageBucket("UNKNOWN")
                .stateCode("UNKNOWN")
                .district("UNKNOWN")
                .occupationCode("UNKNOWN")
                .incomeTier("UNKNOWN")
                .categoryCode("UNKNOWN")
                .genderCode("UNKNOWN")
                .disabilityStatus(null)
                .isFarmer(null)
                .isStudent(null)
                .bplStatus(null)
                .educationLevel("UNKNOWN")
                .build();
    }

    private String determineAgeBucket(Integer age) {
        if (age == null || age <= 0) return "UNKNOWN";
        if (age < 18) return "AGE_BELOW_18";
        if (age <= 25) return "AGE_18_25";
        if (age <= 35) return "AGE_26_35";
        if (age <= 50) return "AGE_36_50";
        if (age <= 60) return "AGE_51_60";
        return "AGE_60_PLUS";
    }

    private String determineIncomeTier(Double income, Boolean bpl) {
        if (Boolean.TRUE.equals(bpl)) return "BPL";
        if (income == null) return "UNKNOWN";
        if (income <= 100000.0) return "TIER_0_1L";
        if (income <= 300000.0) return "TIER_1_3L";
        if (income <= 500000.0) return "TIER_3_5L";
        if (income <= 800000.0) return "TIER_5_8L";
        return "TIER_ABOVE_8L";
    }

    private String normalizeString(String val) {
        if (val == null || val.isBlank()) return "UNKNOWN";
        return val.trim().toUpperCase().replace(" ", "_");
    }
}

