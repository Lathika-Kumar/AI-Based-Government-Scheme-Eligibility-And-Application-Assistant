package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.*;

/**
 * Phase 38: Standardized Training Example Contract.
 * Enforces strict separation between input features and target labels to prevent target leakage.
 * Invariant: Can ONLY be instantiated for schemes that have passed statutory eligibility.
 * Invariant: Input features must never contain future outcome signals (e.g., SCHEME_APPLIED, APPLICATION_COMPLETED).
 */
@Getter
@ToString
public class TrainingExample {

    private final String exampleId;
    private final String sessionId;
    private final String schemeCode;
    private final TrainingFeatures features;
    private final TrainingLabel label;
    private final boolean statutoryEligible;
    private final Instant createdAt;

    @Getter
    @ToString
    @Builder
    public static class TrainingFeatures {
        private final Map<String, Object> profileAttributes;
        private final Map<String, Object> schemeCharacteristics;
        private final Map<String, Object> recommendationContext;
        private final List<String> priorInteractions;

        public static TrainingFeatures of(
                Map<String, Object> profileAttributes,
                Map<String, Object> schemeCharacteristics,
                Map<String, Object> recommendationContext,
                List<String> priorInteractions
        ) {
            return TrainingFeatures.builder()
                    .profileAttributes(profileAttributes != null ? new LinkedHashMap<>(profileAttributes) : new LinkedHashMap<>())
                    .schemeCharacteristics(schemeCharacteristics != null ? new LinkedHashMap<>(schemeCharacteristics) : new LinkedHashMap<>())
                    .recommendationContext(recommendationContext != null ? new LinkedHashMap<>(recommendationContext) : new LinkedHashMap<>())
                    .priorInteractions(priorInteractions != null ? new ArrayList<>(priorInteractions) : new ArrayList<>())
                    .build();
        }
    }

    @Getter
    @ToString
    @Builder
    public static class TrainingLabel {
        private final String targetEventType;
        private final boolean converted;
        private final RelevanceGrade relevanceGrade;
        private final long targetTimestamp;
    }

    @Builder
    public TrainingExample(
            String exampleId,
            String sessionId,
            String schemeCode,
            TrainingFeatures features,
            TrainingLabel label,
            boolean statutoryEligible,
            Instant createdAt
    ) {
        if (!statutoryEligible) {
            throw new IllegalStateException("Statutory Eligibility Invariant Violated: Cannot construct TrainingExample for an ineligible scheme. " +
                    "EligibilityEngine.evaluate() must confirm ELIGIBLE status before feature extraction.");
        }
        this.exampleId = Objects.requireNonNull(exampleId, "exampleId must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.schemeCode = Objects.requireNonNull(schemeCode, "schemeCode must not be null");
        this.features = Objects.requireNonNull(features, "features must not be null");
        this.label = Objects.requireNonNull(label, "label must not be null");
        this.statutoryEligible = true;
        this.createdAt = createdAt != null ? createdAt : Instant.now();

        // Enforce zero target leakage upon construction
        validateNoTargetLeakage();
    }

    private void validateNoTargetLeakage() {
        TargetLeakageDetector detector = new TargetLeakageDetector();
        if (detector.hasLeakage(features.getProfileAttributes())
                || detector.hasLeakage(features.getSchemeCharacteristics())
                || detector.hasLeakage(features.getRecommendationContext())) {
            throw new IllegalStateException("Target leakage detected in training example features: Features must never contain future outcome signals.");
        }

        // Also ensure prior interactions do not contain post-target events
        if (features.getPriorInteractions() != null) {
            for (String event : features.getPriorInteractions()) {
                if ("APPLICATION_COMPLETED".equalsIgnoreCase(event)
                        || "SCHEME_APPLIED".equalsIgnoreCase(event)) {
                    throw new IllegalStateException("Target leakage in prior interactions: Terminal conversion event '" + event + "' cannot be a prior feature.");
                }
            }
        }
    }
}
