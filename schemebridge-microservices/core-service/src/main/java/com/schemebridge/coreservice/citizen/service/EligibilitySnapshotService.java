package com.schemebridge.coreservice.citizen.service;

import com.schemebridge.coreservice.citizen.model.CitizenDocument;
import com.schemebridge.coreservice.citizen.model.EligibilitySnapshot;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service("citizenEligibilitySnapshotService")
public class EligibilitySnapshotService {

    public EligibilitySnapshot calculateEligibilitySnapshot(CitizenDocument doc) {
        double score = calculateEligibilityScore(doc);
        int matched = matchedSchemes(doc);
        LocalDateTime now = LocalDateTime.now();

        return EligibilitySnapshot.builder()
                .eligibilityScore(score)
                .matchedSchemes(matched)
                .lastCalculated(now)
                .build();
    }

    public double calculateEligibilityScore(CitizenDocument doc) {
        if (doc.getProfileCompletion() == null) {
            return 0.0;
        }
        double baseScore = doc.getProfileCompletion().getCompletionPercentage();
        if (doc.getSpecialCategoryDetails() != null) {
            if (Boolean.TRUE.equals(doc.getSpecialCategoryDetails().getIsFarmer())) baseScore += 5.0;
            if (Boolean.TRUE.equals(doc.getSpecialCategoryDetails().getIsDisabled())) baseScore += 5.0;
            if (Boolean.TRUE.equals(doc.getSpecialCategoryDetails().getIsMinority())) baseScore += 5.0;
        }
        return Math.min(100.0, Math.round(baseScore * 100.0) / 100.0);
    }

    public int matchedSchemes(CitizenDocument doc) {
        if (doc.getProfileCompletion() == null || doc.getProfileCompletion().getCompletionPercentage() < 30.0) {
            return 2; // Baseline public schemes
        } else if (doc.getProfileCompletion().getCompletionPercentage() < 70.0) {
            return 8;
        } else {
            return 15;
        }
    }
}
