package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 34: Model Offline Evaluation Report DTO.
 * Records evaluation metrics against the frozen test set and baseline comparison results.
 */
@Getter
@ToString
@Builder
public class Phase34EvaluationReport {

    private final String phase;
    private final String evaluationStatus; // PASS, INSUFFICIENT_DATA, FAIL
    private final String baselineModel; // "2.2.0-hybrid-semantic-384d"
    private final String fallbackModel; // "1.0.0-deterministic"
    private final String candidateModel; // "candidate-model-v3" or "NONE"
    private final String comparisonResult; // CANDIDATE_IMPROVES, NO_SIGNIFICANT_IMPROVEMENT, INSUFFICIENT_DATA
    private final Double precision;
    private final Double recall;
    private final Double f1;
    private final Double accuracy;
    private final String rankingQuality;
    private final long testSessionCount;
    private final long positiveOutcomeCount;
    private final long negativeOutcomeCount;
    private final Map<String, Long> classDistribution;
    private final String governanceStatus; // "PENDING_HUMAN_GOVERNANCE"
    private final boolean modelPromotionAllowed; // Strictly false
    private final Instant evaluatedAt;
}
