package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 31: Machine-readable outcome accumulation progress report contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase31OutcomeProgressReport {

    private long legitimateOutcomeSessions;
    private long requiredOutcomeSessions;
    private long remainingOutcomeSessions;
    private double progressPercent;
    private String trainingStatus; // "TRAINING_NOT_READY" or "TRAINING_READY"

    private boolean trainingReady;
    private boolean modelTrainingAllowed;
    private boolean modelPromotionAllowed;

    private Map<String, Long> relevanceGradesBreakdown;
    private Map<String, Long> excludedTelemetry;

    private Instant generatedAt;
}
