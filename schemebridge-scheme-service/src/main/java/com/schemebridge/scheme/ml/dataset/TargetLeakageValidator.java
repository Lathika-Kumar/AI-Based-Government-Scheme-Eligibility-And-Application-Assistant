package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Phase 34: Strict Target Leakage Validator.
 * Audits feature vectors and ranking contexts to guarantee that target outcomes,
 * terminal conversion states, post-ranking behavioral signals, and labels never contaminate
 * the predictive ranking feature vector.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TargetLeakageValidator {

    private static final Set<String> FORBIDDEN_TARGET_KEYS = Set.of(
            "schemeapplied", "scheme_applied",
            "applicationcompleted", "application_completed",
            "converted", "conversion",
            "terminalgrade", "terminal_grade",
            "outcomelabel", "outcome_label", "outcome",
            "conversionstatus", "conversion_status",
            "applicationstatus", "application_status",
            "relevancegrade", "relevance_grade",
            "relevancescore", "relevance_score",
            "clickoutcome", "click_outcome",
            "target", "label", "postrankingbehavior"
    );

    @Getter
    @ToString
    @Builder
    public static class LeakageValidationResult {
        private final boolean passed;
        private final int violationCount;
        private final List<String> leakedKeys;
    }

    /**
     * Validates ranking context for target label leakage.
     */
    public LeakageValidationResult validateContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return LeakageValidationResult.builder()
                    .passed(true)
                    .violationCount(0)
                    .leakedKeys(List.of())
                    .build();
        }

        List<String> leaked = new ArrayList<>();
        for (String key : context.keySet()) {
            if (key == null) continue;
            String normalized = key.toLowerCase().replace("-", "").replace("_", "").trim();
            for (String forbidden : FORBIDDEN_TARGET_KEYS) {
                String normForbidden = forbidden.replace("_", "");
                if (normalized.equals(normForbidden) || normalized.contains(normForbidden)) {
                    leaked.add(key);
                    break;
                }
            }
        }

        boolean passed = leaked.isEmpty();
        if (!passed) {
            log.warn("Target leakage detected in ranking context: {}", leaked);
        }

        return LeakageValidationResult.builder()
                .passed(passed)
                .violationCount(leaked.size())
                .leakedKeys(leaked)
                .build();
    }

    /**
     * Validates a UserSchemeFeatureVector for target leakage.
     */
    public LeakageValidationResult validate(UserSchemeFeatureVector featureVector) {
        if (featureVector == null) {
            return LeakageValidationResult.builder().passed(true).violationCount(0).leakedKeys(List.of()).build();
        }
        return validateContext(featureVector.getRankingContext());
    }
}
