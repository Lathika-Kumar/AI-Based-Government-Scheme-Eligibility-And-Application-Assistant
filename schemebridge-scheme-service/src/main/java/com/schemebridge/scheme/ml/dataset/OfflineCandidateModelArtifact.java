package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Phase 40: Immutable Offline Candidate Model Artifact.
 * Represents an offline-trained candidate model produced exclusively from a qualified, frozen dataset snapshot.
 *
 * CRITICAL ARCHITECTURAL INVARIANTS:
 * - Active production model remains "2.2.0-hybrid-semantic-384d".
 * - Fallback production model remains "1.0.0-deterministic".
 * - candidateModelVersion is designated as "offline-candidate-<datasetVersion>" and NEVER active.
 * - modelPromotionAllowed is PERMANENTLY false (strictly reserved for human governance).
 * - isProductionActive is strictly false.
 */
@Getter
@ToString
public class OfflineCandidateModelArtifact {

    public static final String BASELINE_MODEL_VERSION = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL_VERSION = "1.0.0-deterministic";

    private final String candidateModelVersion;
    private final String baselineModelVersion;
    private final String fallbackModelVersion;
    private final String datasetVersion;
    private final String datasetSha256Digest;
    private final long trainingExampleCount;
    private final String algorithm;
    private final String objective;
    private final Map<String, Object> hyperparameters;
    private final Map<String, Double> featureWeights;
    private final String evaluationStatus; // PENDING_EVALUATION, EVALUATED, INSUFFICIENT_DATA
    private final boolean modelPromotionAllowed; // Always false
    private final boolean isProductionActive; // Always false
    private final Instant trainedAt;
    private final String statusMessage;

    @Builder
    public OfflineCandidateModelArtifact(
            String candidateModelVersion,
            String baselineModelVersion,
            String fallbackModelVersion,
            String datasetVersion,
            String datasetSha256Digest,
            long trainingExampleCount,
            String algorithm,
            String objective,
            Map<String, Object> hyperparameters,
            Map<String, Double> featureWeights,
            String evaluationStatus,
            boolean modelPromotionAllowed,
            boolean isProductionActive,
            Instant trainedAt,
            String statusMessage
    ) {
        this.candidateModelVersion = candidateModelVersion != null ? candidateModelVersion : "offline-candidate-NONE";
        this.baselineModelVersion = BASELINE_MODEL_VERSION;
        this.fallbackModelVersion = FALLBACK_MODEL_VERSION;
        this.datasetVersion = datasetVersion != null ? datasetVersion : "NONE";
        this.datasetSha256Digest = datasetSha256Digest != null ? datasetSha256Digest : "NONE";
        this.trainingExampleCount = trainingExampleCount;
        this.algorithm = algorithm != null ? algorithm : "LambdaMART-LTR";
        this.objective = objective != null ? objective : "pairwise-cross-entropy";
        this.hyperparameters = hyperparameters != null ? Collections.unmodifiableMap(hyperparameters) : Collections.emptyMap();
        this.featureWeights = featureWeights != null ? Collections.unmodifiableMap(featureWeights) : Collections.emptyMap();
        this.evaluationStatus = evaluationStatus != null ? evaluationStatus : "PENDING_EVALUATION";
        this.modelPromotionAllowed = false; // Hard permanent invariant
        this.isProductionActive = false; // Hard permanent invariant
        this.trainedAt = trainedAt != null ? trainedAt : Instant.now();
        this.statusMessage = statusMessage != null ? statusMessage : "";
    }
}
