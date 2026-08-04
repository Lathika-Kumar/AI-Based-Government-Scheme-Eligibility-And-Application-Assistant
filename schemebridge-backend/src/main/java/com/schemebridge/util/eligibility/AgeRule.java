package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AgeRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 20;

    @Override
    public String getRuleName() {
        return "Age Criteria";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        Integer minAge = scheme.getMinimumAge();
        Integer maxAge = scheme.getMaximumAge();

        if (minAge == null && maxAge == null) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Age criteria eligible (no restriction)")
                    .build();
        }

        Integer citizenAge = profile != null ? profile.getAge() : null;
        if (citizenAge == null) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("Age information is missing in profile")
                    .missingField("dateOfBirthOrAge")
                    .build();
        }

        boolean minPass = (minAge == null || citizenAge >= minAge);
        boolean maxPass = (maxAge == null || citizenAge <= maxAge);

        if (minPass && maxPass) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("Age eligible (" + citizenAge + " years old)")
                    .build();
        } else {
            String reason = "Age " + citizenAge + " is outside scheme range ("
                    + (minAge != null ? minAge : 0) + " - " + (maxAge != null ? maxAge : 110) + " years)";
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation(reason)
                    .build();
        }
    }
}
