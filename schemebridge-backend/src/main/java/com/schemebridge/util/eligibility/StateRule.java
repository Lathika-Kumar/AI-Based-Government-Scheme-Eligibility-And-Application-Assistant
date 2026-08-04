package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import com.schemebridge.enums.SchemeType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StateRule implements EligibilityRuleEvaluator {

    private static final int MAX_WEIGHT = 15;

    @Override
    public String getRuleName() {
        return "State Eligibility";
    }

    @Override
    public RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme) {
        List<String> applicableStates = scheme.getApplicableStates();

        if (scheme.getSchemeType() == SchemeType.CENTRAL || applicableStates == null || applicableStates.isEmpty() || applicableStates.contains("All India") || applicableStates.contains("ALL")) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("State eligible (Pan-India / Central Scheme)")
                    .build();
        }

        String citizenState = profile != null ? profile.getState() : null;
        if (citizenState == null || citizenState.isBlank()) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation("State details missing in profile")
                    .missingField("state")
                    .build();
        }

        boolean matched = applicableStates.stream()
                .anyMatch(s -> s.equalsIgnoreCase(citizenState.trim()));

        if (matched) {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(true)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(MAX_WEIGHT)
                    .explanation("State eligible (" + citizenState + " resident)")
                    .build();
        } else {
            return RuleEvaluationResult.builder()
                    .ruleName(getRuleName())
                    .passed(false)
                    .maxWeight(MAX_WEIGHT)
                    .awardedScore(0)
                    .explanation(citizenState + " is not in scheme applicable states " + applicableStates)
                    .build();
        }
    }
}
