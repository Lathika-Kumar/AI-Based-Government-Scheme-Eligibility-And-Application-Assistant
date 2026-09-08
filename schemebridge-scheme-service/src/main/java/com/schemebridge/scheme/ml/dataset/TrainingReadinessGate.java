package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Phase 29: Centralized deterministic training readiness gate.
 * Evaluates whether genuine citizen outcome volume is sufficient for supervised LTR training.
 * Invariant: Even when outcome threshold (100) is reached, model promotion remains strictly forbidden.
 */
@Slf4j
@Component
public class TrainingReadinessGate {

    public static final long OUTCOME_SESSION_THRESHOLD = 100L;

    @Getter
    @ToString
    @Builder
    public static class GateDecision {
        private final TrainingReadinessStatus status;
        private final boolean modelTrainingAllowed;
        private final boolean modelPromotionAllowed;
        private final long legitimateOutcomeSessions;
        private final long requiredOutcomeSessions;
        private final long remainingOutcomeSessions;
        private final String reason;
    }

    public GateDecision evaluate(long legitimateOutcomeSessions) {
        if (legitimateOutcomeSessions < OUTCOME_SESSION_THRESHOLD) {
            long remaining = OUTCOME_SESSION_THRESHOLD - legitimateOutcomeSessions;
            String reason = String.format("Insufficient legitimate outcome sessions: %d found (required: %d, remaining: %d). Model training blocked.",
                    legitimateOutcomeSessions, OUTCOME_SESSION_THRESHOLD, remaining);
            log.info("Readiness Gate Decision: TRAINING_NOT_READY - {}", reason);
            return GateDecision.builder()
                    .status(TrainingReadinessStatus.TRAINING_NOT_READY)
                    .modelTrainingAllowed(false)
                    .modelPromotionAllowed(false)
                    .legitimateOutcomeSessions(legitimateOutcomeSessions)
                    .requiredOutcomeSessions(OUTCOME_SESSION_THRESHOLD)
                    .remainingOutcomeSessions(remaining)
                    .reason(reason)
                    .build();
        } else {
            String reason = String.format("Legitimate outcome volume met (%d >= %d). Model training permitted in offline sandbox, but model promotion remains blocked.",
                    legitimateOutcomeSessions, OUTCOME_SESSION_THRESHOLD);
            log.info("Readiness Gate Decision: TRAINING_READY - {}", reason);
            return GateDecision.builder()
                    .status(TrainingReadinessStatus.TRAINING_READY)
                    .modelTrainingAllowed(true)
                    .modelPromotionAllowed(false) // Never automatically promote model
                    .legitimateOutcomeSessions(legitimateOutcomeSessions)
                    .requiredOutcomeSessions(OUTCOME_SESSION_THRESHOLD)
                    .remainingOutcomeSessions(0L)
                    .reason(reason)
                    .build();
        }
    }
}
