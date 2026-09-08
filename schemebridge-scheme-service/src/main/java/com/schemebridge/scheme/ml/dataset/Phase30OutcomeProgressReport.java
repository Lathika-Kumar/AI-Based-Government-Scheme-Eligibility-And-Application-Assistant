package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 30: Machine-readable outcome accumulation progress report contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase30OutcomeProgressReport {

    private long legitimateOutcomeSessions;
    private long requiredOutcomeSessions;
    private long remainingOutcomeSessions;
    private double progressPercent;
    private String trainingStatus; // "TRAINING_NOT_READY" or "TRAINING_READY"

    private Map<String, Long> relevanceGradesBreakdown;
    private Map<String, Long> excludedTelemetry;

    private Instant generatedAt;
}
