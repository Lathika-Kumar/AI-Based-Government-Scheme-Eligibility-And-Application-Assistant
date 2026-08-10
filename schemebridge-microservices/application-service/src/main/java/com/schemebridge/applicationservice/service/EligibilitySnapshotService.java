package com.schemebridge.applicationservice.service;

import com.schemebridge.applicationservice.document.ApplicationDocument;
import com.schemebridge.applicationservice.document.EligibilitySnapshot;
import com.schemebridge.applicationservice.dto.CreateApplicationRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * EligibilitySnapshotService captures an immutable snapshot of a citizen's
 * eligibility criteria at the exact moment of application creation.
 *
 * This snapshot is critical for audit purposes — it records what the citizen's
 * profile looked like WHEN they applied, even if the profile changes later.
 *
 * Future integration: When citizen-service and scheme-service are called,
 * this service will enrich the snapshot with live data from both services.
 */
@Service
public class EligibilitySnapshotService {

    /**
     * Builds an EligibilitySnapshot from the CreateApplicationRequest.
     * Called once during application creation and never updated.
     */
    public EligibilitySnapshot captureSnapshot(CreateApplicationRequest request) {
        return EligibilitySnapshot.builder()
                .category(request.getCategory())
                .gender(request.getGender())
                .age(request.getAge())
                .annualIncome(request.getAnnualIncome())
                .state(request.getState())
                .district(request.getDistrict())
                .occupationType(request.getOccupationType())
                .isFarmer(request.getIsFarmer())
                .isDisabled(request.getIsDisabled())
                .isMinority(request.getIsMinority())
                .isStudent(request.getIsStudent())
                .isWidow(request.getIsWidow())
                .isSeniorCitizen(request.getIsSeniorCitizen())
                .eligibilityScore(request.getEligibilityScore())
                .matchedRules(request.getMatchedRules())
                .schemeVersion(request.getSchemeVersion())
                .capturedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Checks if the citizen's eligibility score meets the minimum threshold.
     * Schemes with no rules (score = null) are universally applicable.
     *
     * @param snapshot the eligibility snapshot
     * @param minimumScore minimum required score (default: 0.0 for all)
     * @return true if eligible
     */
    public boolean meetsMinimumEligibility(EligibilitySnapshot snapshot, double minimumScore) {
        if (snapshot == null || snapshot.getEligibilityScore() == null) {
            return true; // No score means universally applicable
        }
        return snapshot.getEligibilityScore() >= minimumScore;
    }
}
