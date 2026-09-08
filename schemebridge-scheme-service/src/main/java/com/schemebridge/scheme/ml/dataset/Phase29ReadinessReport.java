package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Machine-readable training readiness report contract for Phase 29.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase29ReadinessReport {

    @Builder.Default
    private int phase = 29;

    @Builder.Default
    private String status = "VALIDATED";

    private TrainingReadinessStatus trainingReadiness;
    private long legitimateOutcomeSessions;
    private long requiredOutcomeSessions;
    private long remainingOutcomeSessions;
    private long syntheticFixturesQuarantined;
    private long piiViolations;
    private long usableTrainingPairs;

    private boolean modelTrainingAllowed;
    private boolean modelPromotionAllowed;

    private String activeModel;
    private String fallbackModel;
    private int circuitBreakerMs;
    private double statutoryEligibilityViolationRate;

    private Map<String, Integer> mongoMutations;
    private Instant generatedAt;
}
