package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Phase 29: Feature / Label Leakage Audit Detector.
 * Strictly verifies that input feature vectors do NOT contain target labels, outcomes,
 * completion states, or post-ranking behavioral signals.
 */
@Slf4j
@Component
public class TargetLeakageDetector {

    private static final Set<String> FORBIDDEN_LEAKAGE_KEYS = Set.of(
            "relevancegrade", "relevance_grade", "relevancescore", "relevance_score",
            "applicationoutcome", "application_outcome", "outcome",
            "clickoutcome", "click_outcome", "converted", "conversion",
            "eventtype", "event_type", "target", "label",
            "futuretimestamp", "future_timestamp",
            "applicationcompletionstate", "application_completed", "applicationcompleted",
            "postrankingbehavior", "post_ranking",
            "schemeapplied", "scheme_applied",
            "futureapplicationstatus", "future_application_status",
            "futureapprovalstatus", "future_approval_status",
            "approvalstatus", "approval_status",
            "terminaloutcomegrade", "terminal_outcome_grade",
            "futureoutcome", "future_outcome"
    );

    /**
     * Audits a feature vector and its ranking context for target label leakage.
     * Throws IllegalStateException if any target or outcome information is found leaked into features.
     */
    public void audit(UserSchemeFeatureVector featureVector) {
        if (featureVector == null) {
            return;
        }

        Map<String, Object> rankingContext = featureVector.getRankingContext();
        if (rankingContext != null && !rankingContext.isEmpty()) {
            for (String key : rankingContext.keySet()) {
                if (key == null) continue;
                String normalized = key.toLowerCase().replace("-", "").replace("_", "").trim();
                for (String forbidden : FORBIDDEN_LEAKAGE_KEYS) {
                    String normForbidden = forbidden.replace("_", "");
                    if (normalized.equals(normForbidden) || normalized.contains(normForbidden)) {
                        String msg = String.format("CRITICAL TARGET LEAKAGE DETECTED: Feature vector ranking context contains forbidden target key '%s'", key);
                        log.error(msg);
                        throw new IllegalStateException(msg);
                    }
                }
            }
        }
    }

    /**
     * Returns true if any forbidden leakage key exists in the context map.
     */
    public boolean hasLeakage(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return false;
        for (String key : context.keySet()) {
            if (key == null) continue;
            String normalized = key.toLowerCase().replace("-", "").replace("_", "").trim();
            for (String forbidden : FORBIDDEN_LEAKAGE_KEYS) {
                String normForbidden = forbidden.replace("_", "");
                if (normalized.equals(normForbidden) || normalized.contains(normForbidden)) {
                    return true;
                }
            }
        }
        return false;
    }
}
