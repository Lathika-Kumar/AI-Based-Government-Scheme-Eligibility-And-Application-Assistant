package com.schemebridge.mapper;

import com.schemebridge.dto.ProfileResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toProfileResponse(User user, CitizenProfile profile) {
        if (user == null && profile == null) {
            return null;
        }

        String maskedAadhaar = null;
        if (profile != null && profile.getAadhaarNumber() != null && profile.getAadhaarNumber().length() >= 4) {
            String raw = profile.getAadhaarNumber().replaceAll("\\D", "");
            if (raw.length() >= 4) {
                maskedAadhaar = "XXXX-XXXX-" + raw.substring(raw.length() - 4);
            }
        }

        String maskedPan = null;
        if (profile != null && profile.getPanNumber() != null && profile.getPanNumber().length() >= 4) {
            String raw = profile.getPanNumber().trim().toUpperCase();
            if (raw.length() == 10) {
                maskedPan = "XXXXX" + raw.substring(5);
            } else {
                maskedPan = "XXXXX" + raw.substring(Math.max(0, raw.length() - 4));
            }
        }

        return ProfileResponse.builder()
                .id(profile != null ? profile.getId() : null)
                .userId(user != null ? user.getId() : (profile != null ? profile.getUserId() : null))
                .email(user != null ? user.getEmail() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .age(profile != null ? profile.getAge() : null)
                .gender(profile != null ? profile.getGender() : null)
                .maritalStatus(profile != null ? profile.getMaritalStatus() : null)
                .state(profile != null ? profile.getState() : null)
                .district(profile != null ? profile.getDistrict() : null)
                .cityOrVillage(profile != null ? profile.getCityOrVillage() : null)
                .pincode(profile != null ? profile.getPincode() : null)
                .annualIncome(profile != null ? profile.getAnnualIncome() : null)
                .category(profile != null ? profile.getCategory() : null)
                .occupation(profile != null ? profile.getOccupation() : null)
                .employmentStatus(profile != null ? profile.getEmploymentStatus() : null)
                .disabilityStatus(profile != null ? profile.getDisabilityStatus() : Boolean.FALSE)
                .qualification(profile != null ? profile.getQualification() : null)
                .maskedAadhaarNumber(maskedAadhaar)
                .maskedPanNumber(maskedPan)
                .profileVersion(profile != null ? profile.getProfileVersion() : 1)
                .lastUpdatedAt(profile != null ? profile.getLastUpdatedAt() : null)
                .lastUpdatedBy(profile != null ? profile.getLastUpdatedBy() : null)
                .profileCompletedAt(profile != null ? profile.getProfileCompletedAt() : null)
                .onboardingCompleted(user != null ? Boolean.TRUE.equals(user.getOnboardingCompleted()) : false)
                .createdAt(profile != null ? profile.getCreatedAt() : null)
                .build();
    }
}
