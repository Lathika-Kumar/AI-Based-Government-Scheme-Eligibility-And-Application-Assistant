package com.schemebridge.citizenservice.service;

import com.schemebridge.citizenservice.document.CitizenDocument;
import com.schemebridge.citizenservice.document.ProfileCompletion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProfileCompletionService {

    public ProfileCompletion calculateCompletion(CitizenDocument doc) {
        List<String> missingFields = missingFields(doc);
        int totalSections = 10;
        int completedSections = totalSections - missingFields.size();
        double percentage = Math.round(((double) completedSections / totalSections) * 100.0 * 100.0) / 100.0;
        boolean isComplete = missingFields.isEmpty();

        return ProfileCompletion.builder()
                .completionPercentage(percentage)
                .completedSections(completedSections)
                .totalSections(totalSections)
                .missingFields(missingFields)
                .isComplete(isComplete)
                .build();
    }

    public List<String> missingFields(CitizenDocument doc) {
        List<String> missing = new ArrayList<>();

        if (doc.getPersonalDetails() == null || doc.getPersonalDetails().getFullName() == null || doc.getPersonalDetails().getFullName().isBlank()) {
            missing.add("Personal Details: Full Name");
        }
        if (doc.getPersonalDetails() == null || doc.getPersonalDetails().getDateOfBirth() == null) {
            missing.add("Personal Details: Date of Birth");
        }
        if (doc.getPersonalDetails() == null || doc.getPersonalDetails().getGender() == null) {
            missing.add("Personal Details: Gender");
        }
        if (doc.getPersonalDetails() == null || doc.getPersonalDetails().getCategory() == null) {
            missing.add("Personal Details: Category");
        }
        if (doc.getAddressDetails() == null || doc.getAddressDetails().getState() == null || doc.getAddressDetails().getState().isBlank()) {
            missing.add("Address: State");
        }
        if (doc.getAddressDetails() == null || doc.getAddressDetails().getDistrict() == null || doc.getAddressDetails().getDistrict().isBlank()) {
            missing.add("Address: District");
        }
        if (doc.getAddressDetails() == null || doc.getAddressDetails().getPincode() == null || doc.getAddressDetails().getPincode().isBlank()) {
            missing.add("Address: Pincode");
        }
        if (doc.getIncomeDetails() == null || doc.getIncomeDetails().getAnnualIncome() == null) {
            missing.add("Income Details: Annual Income");
        }
        if (doc.getOccupationDetails() == null || doc.getOccupationDetails().getOccupationType() == null) {
            missing.add("Occupation Details: Occupation Type");
        }
        if (doc.getEducationDetails() == null || doc.getEducationDetails().getQualification() == null) {
            missing.add("Education Details: Qualification");
        }

        return missing;
    }
}
