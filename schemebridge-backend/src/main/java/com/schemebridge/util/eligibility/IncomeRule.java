package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class IncomeRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 20;

    @Override
    public String getRuleName() {
        return "Income Limit";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        BigDecimal minIncome = scheme.getMinimumIncome();
        BigDecimal maxIncome = scheme.getMaximumIncome();

        if (minIncome == null && maxIncome == null) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Income limit eligible (no restriction)")
                    .build();
        }

        BigDecimal citizenIncome = profile != null ? profile.getAnnualIncome() : null;
        if (citizenIncome == null) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Annual income details missing in profile")
                    .missingField("annualIncome")
                    .build();
        }

        boolean minPass = (minIncome == null || citizenIncome.compareTo(minIncome) >= 0);
        boolean maxPass = (maxIncome == null || citizenIncome.compareTo(maxIncome) <= 0);

        if (minPass && maxPass) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Income within limit (₹" + citizenIncome.toPlainString() + ")")
                    .build();
        } else {
            String limitDesc = maxIncome != null ? "₹" + maxIncome.toPlainString() : "unlimited";
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Annual income ₹" + citizenIncome.toPlainString() + " exceeds limit (" + limitDesc + ")")
                    .build();
        }
    }
}
