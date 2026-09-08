package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 30: Machine-readable training readiness report contract.
 * Strictly adheres to Phase 30 specifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase30ReadinessReport {

    @Builder.Default
    private int phase = 30;

    private String status; // "TRAINING_NOT_READY" or "TRAINING_READY"
    private long legitimateOutcomeSessions;
    private long requiredThreshold; // 100
    private long remainingOutcomeSessions;

    private boolean trainingReady;
    private boolean modelTrainingAllowed;
    private boolean modelPromotionAllowed; // strictly false in Phase 30

    private long syntheticFixturesQuarantined; // 29 historical fixtures
    private long piiViolations;
    private double statutoryEligibilityViolationRate; // 0.0

    private Map<String, Integer> databaseMutations; // INSERT: 0, UPDATE: 0, DELETE: 0, DROP: 0

    private String activeModel; // "2.2.0-hybrid-semantic-384d"
    private String fallbackModel; // "1.0.0-deterministic"
    private int circuitBreakerMs; // 200

    private Instant generatedAt;
}
