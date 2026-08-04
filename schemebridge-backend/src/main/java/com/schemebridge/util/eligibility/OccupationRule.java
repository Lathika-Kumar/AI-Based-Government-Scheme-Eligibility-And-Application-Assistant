package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OccupationRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 10;

    @Override
    public String getRuleName() {
        return "Occupation Criteria";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        List<String> allowedOccupations = scheme.getAllowedOccupations();

        if (allowedOccupations == null || allowedOccupations.isEmpty() || allowedOccupations.contains("ALL") || allowedOccupations.contains("Any") || allowedOccupations.contains("All")) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Occupation criteria eligible (Open to all occupations)")
                    .build();
        }

        String citizenOccupation = profile != null ? profile.getOccupation() : null;
        if (citizenOccupation == null || citizenOccupation.isBlank()) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Occupation details missing in profile")
                    .missingField("occupation")
                    .build();
        }

        boolean matched = allowedOccupations.stream()
                .anyMatch(o -> o.equalsIgnoreCase(citizenOccupation.trim()) || o.toLowerCase().contains(citizenOccupation.trim().toLowerCase()));

        if (matched) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Occupation eligible (" + citizenOccupation + ")")
                    .build();
        } else {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Occupation " + citizenOccupation + " does not match allowed occupations " + allowedOccupations)
                    .build();
        }
    }
}
