package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 34: Model Training Report DTO.
 * Records training execution results, frozen dataset version, and candidate model details.
 */
@Getter
@ToString
@Builder
public class Phase34TrainingReport {

    private final String phase;
    private final String trainingStatus; // BLOCKED_BY_READINESS, TRAINED, FAILED
    private final String candidateModelVersion; // "candidate-model-v3" or "NONE"
    private final String activeModel; // "2.2.0-hybrid-semantic-384d"
    private final String fallbackModel; // "1.0.0-deterministic"
    private final int circuitBreakerMs;
    private final String datasetVersion;
    private final long legitimateOutcomeSessions;
    private final long requiredThreshold;
    private final boolean modelTrainingAllowed;
    private final boolean modelPromotionAllowed; // Strictly false
    private final String governanceState; // "PENDING_HUMAN_GOVERNANCE" or "NONE"
    private final Map<String, Object> trainingMetadata;
    private final Instant generatedAt;
}
