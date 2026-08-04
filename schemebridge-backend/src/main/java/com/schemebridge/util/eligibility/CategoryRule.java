package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 15;

    @Override
    public String getRuleName() {
        return "Category Eligibility";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        List<String> allowedCategories = scheme.getAllowedCategories();

        if (allowedCategories == null || allowedCategories.isEmpty() || allowedCategories.contains("ALL") || allowedCategories.contains("General")) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Social Category eligible (Open to all categories)")
                    .build();
        }

        String citizenCategory = profile != null ? profile.getCategory() : null;
        if (citizenCategory == null || citizenCategory.isBlank()) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Category details missing in profile")
                    .missingField("category")
                    .build();
        }

        boolean matched = allowedCategories.stream()
                .anyMatch(c -> c.equalsIgnoreCase(citizenCategory.trim()));

        if (matched) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Category eligible (" + citizenCategory + ")")
                    .build();
        } else {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Social Category " + citizenCategory + " does not match allowed categories " + allowedCategories)
                    .build();
        }
    }
}
