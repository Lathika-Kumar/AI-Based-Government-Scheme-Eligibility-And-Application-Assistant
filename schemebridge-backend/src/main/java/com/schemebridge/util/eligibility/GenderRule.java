package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenderRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 10;

    @Override
    public String getRuleName() {
        return "Gender Criteria";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        List<String> allowedGenders = scheme.getAllowedGenders();

        if (allowedGenders == null || allowedGenders.isEmpty() || allowedGenders.contains("ALL") || allowedGenders.contains("Any") || allowedGenders.contains("All")) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Gender criteria eligible (All genders)")
                    .build();
        }

        String citizenGender = profile != null ? profile.getGender() : null;
        if (citizenGender == null || citizenGender.isBlank()) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Gender details missing in profile")
                    .missingField("gender")
                    .build();
        }

        boolean matched = allowedGenders.stream()
                .anyMatch(g -> g.equalsIgnoreCase(citizenGender.trim()));

        if (matched) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Gender eligible (" + citizenGender + ")")
                    .build();
        } else {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Gender " + citizenGender + " does not match scheme allowed genders " + allowedGenders)
                    .build();
        }
    }
}
