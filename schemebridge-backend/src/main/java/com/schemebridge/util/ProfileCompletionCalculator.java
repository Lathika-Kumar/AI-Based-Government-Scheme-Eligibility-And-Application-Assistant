package com.schemebridge.util;

import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.User;
import com.schemebridge.enums.ProfileCompletionStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProfileCompletionCalculator {

    public ProfileCompletionResponse calculateCompletion(User user, CitizenProfile profile) {
        List<String> completedFields = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();

        // 1. Personal Section (25 points)
        int personalScore = 0;
        String fullName = user != null && user.getFullName() != null ? user.getFullName().trim() : null;
        if (fullName != null && !fullName.isEmpty()) {
            personalScore += 10;
            completedFields.add("fullName");
        } else {
            missingFields.add("fullName");
        }

        if (profile != null && (profile.getDateOfBirth() != null || profile.getAge() != null)) {
            personalScore += 5;
            completedFields.add("dateOfBirthOrAge");
        } else {
            missingFields.add("dateOfBirthOrAge");
        }

        if (profile != null && profile.getGender() != null && !profile.getGender().isBlank()) {
            personalScore += 5;
            completedFields.add("gender");
        } else {
            missingFields.add("gender");
        }

        if (profile != null && profile.getMaritalStatus() != null && !profile.getMaritalStatus().isBlank()) {
            personalScore += 5;
            completedFields.add("maritalStatus");
        } else {
            missingFields.add("maritalStatus");
        }

        // 2. Location Section (25 points)
        int locationScore = 0;
        if (profile != null && profile.getState() != null && !profile.getState().isBlank()) {
            locationScore += 10;
            completedFields.add("state");
        } else {
            missingFields.add("state");
        }

        if (profile != null && profile.getDistrict() != null && !profile.getDistrict().isBlank()) {
            locationScore += 8;
            completedFields.add("district");
        } else {
            missingFields.add("district");
        }

        if (profile != null && profile.getCityOrVillage() != null && !profile.getCityOrVillage().isBlank()) {
            locationScore += 4;
            completedFields.add("cityOrVillage");
        } else {
            missingFields.add("cityOrVillage");
        }

        if (profile != null && profile.getPincode() != null && !profile.getPincode().isBlank()) {
            locationScore += 3;
            completedFields.add("pincode");
        } else {
            missingFields.add("pincode");
        }

        // 3. Socio-Economic Section (25 points)
        int socioScore = 0;
        if (profile != null && profile.getAnnualIncome() != null) {
            socioScore += 10;
            completedFields.add("annualIncome");
        } else {
            missingFields.add("annualIncome");
        }

        if (profile != null && profile.getCategory() != null && !profile.getCategory().isBlank()) {
            socioScore += 5;
            completedFields.add("category");
        } else {
            missingFields.add("category");
        }

        if (profile != null && profile.getOccupation() != null && !profile.getOccupation().isBlank()) {
            socioScore += 5;
            completedFields.add("occupation");
        } else {
            missingFields.add("occupation");
        }

        if (profile != null && profile.getEmploymentStatus() != null && !profile.getEmploymentStatus().isBlank()) {
            socioScore += 5;
            completedFields.add("employmentStatus");
        } else {
            missingFields.add("employmentStatus");
        }

        // 4. Education Section (15 points)
        int eduScore = 0;
        if (profile != null && profile.getQualification() != null && !profile.getQualification().isBlank()) {
            eduScore += 15;
            completedFields.add("qualification");
        } else {
            missingFields.add("qualification");
        }

        // 5. Identity Documents Section (10 points)
        int identityScore = 0;
        if (profile != null && profile.getAadhaarNumber() != null && !profile.getAadhaarNumber().isBlank()) {
            identityScore += 5;
            completedFields.add("aadhaarNumber");
        } else {
            missingFields.add("aadhaarNumber");
        }

        if (profile != null && profile.getPanNumber() != null && !profile.getPanNumber().isBlank()) {
            identityScore += 5;
            completedFields.add("panNumber");
        } else {
            missingFields.add("panNumber");
        }

        int totalPercentage = Math.min(100, personalScore + locationScore + socioScore + eduScore + identityScore);

        ProfileCompletionStatus status;
        if (totalPercentage >= 100) {
            status = ProfileCompletionStatus.COMPLETED;
        } else if (totalPercentage >= 50) {
            status = ProfileCompletionStatus.PARTIALLY_COMPLETED;
        } else {
            status = ProfileCompletionStatus.INCOMPLETE;
        }

        // Mandatory fields rule for onboardingCompleted
        boolean mandatoryCompleted = isMandatoryComplete(user, profile);
        boolean onboardingCompleted = (user != null && Boolean.TRUE.equals(user.getOnboardingCompleted())) || mandatoryCompleted;

        Map<String, Integer> sectionScores = new HashMap<>();
        sectionScores.put("personal", personalScore);
        sectionScores.put("location", locationScore);
        sectionScores.put("socioEconomic", socioScore);
        sectionScores.put("education", eduScore);
        sectionScores.put("identity", identityScore);

        return ProfileCompletionResponse.builder()
                .completionPercentage(totalPercentage)
                .status(status)
                .sectionScores(sectionScores)
                .completedFields(completedFields)
                .missingFields(missingFields)
                .onboardingCompleted(onboardingCompleted)
                .build();
    }

    public boolean isMandatoryComplete(User user, CitizenProfile profile) {
        if (user == null || user.getFullName() == null || user.getFullName().isBlank()) return false;
        if (profile == null) return false;
        if (profile.getGender() == null || profile.getGender().isBlank()) return false;
        if (profile.getState() == null || profile.getState().isBlank()) return false;
        if (profile.getDistrict() == null || profile.getDistrict().isBlank()) return false;
        if (profile.getAnnualIncome() == null) return false;
        if (profile.getOccupation() == null || profile.getOccupation().isBlank()) return false;
        return profile.getCategory() != null && !profile.getCategory().isBlank();
    }
}
