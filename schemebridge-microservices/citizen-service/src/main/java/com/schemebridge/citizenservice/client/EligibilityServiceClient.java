package com.schemebridge.citizenservice.client;

import com.schemebridge.citizenservice.document.CitizenDocument;
import com.schemebridge.citizenservice.document.EligibilitySnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Placeholder REST client interface for future inter-service integration with Scheme Service (Port 8083).
 * In future modules, this client will issue HTTP/WebClient REST calls to:
 * GET http://localhost:8083/api/v1/schemes/evaluate-eligibility?citizenId={id}
 */
@Component
public class EligibilityServiceClient {

    public EligibilitySnapshot evaluateEligibilityFromSchemeService(CitizenDocument doc) {
        // Placeholder fallback calculation until Scheme Service (Module 6) is deployed
        int matchedSchemesCount = 15;
        double score = 100.0;
        if (doc.getProfileCompletion() != null) {
            score = doc.getProfileCompletion().getCompletionPercentage();
            if (score < 50.0) matchedSchemesCount = 4;
            else if (score < 80.0) matchedSchemesCount = 9;
        }

        return EligibilitySnapshot.builder()
                .eligibilityScore(score)
                .matchedSchemes(matchedSchemesCount)
                .lastCalculated(LocalDateTime.now())
                .build();
    }
}
