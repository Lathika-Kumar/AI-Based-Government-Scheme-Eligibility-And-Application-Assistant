package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 10;

    @Override
    public String getRuleName() {
        return "Required Documents";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        List<String> requiredDocs = scheme.getRequiredDocuments();

        if (requiredDocs == null || requiredDocs.isEmpty()) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("No mandatory documents required")
                    .build();
        }

        // Check if citizen profile has Aadhaar and PAN if required
        boolean hasAadhaar = profile != null && profile.getAadhaarNumber() != null && !profile.getAadhaarNumber().isBlank();
        boolean hasPan = profile != null && profile.getPanNumber() != null && !profile.getPanNumber().isBlank();

        boolean missingCriticalDoc = false;
        String missingDocName = null;

        for (String doc : requiredDocs) {
            String lower = doc.toLowerCase();
            if (lower.contains("aadhaar") && !hasAadhaar) {
                missingCriticalDoc = true;
                missingDocName = "Aadhaar Card";
                break;
            }
            if (lower.contains("pan") && !hasPan) {
                missingCriticalDoc = true;
                missingDocName = "PAN Card";
                break;
            }
        }

        if (!missingCriticalDoc) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Key identity documents available")
                    .build();
        } else {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(5) // Partial credit
                    .explanation("Missing required document: " + missingDocName)
                    .missingDocument(missingDocName)
                    .build();
        }
    }
}
