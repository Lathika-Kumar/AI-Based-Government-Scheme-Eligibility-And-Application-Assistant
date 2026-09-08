package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Phase 39: Immutable Dataset Freeze Snapshot.
 * Represents a cryptographically hashed, versioned, and immutable record of a qualified dataset
 * prepared strictly prior to any offline behavioral training run.
 *
 * Invariant: If outcomes < 100, status must be BLOCKED_BELOW_THRESHOLD and datasetVersion is NONE.
 * Invariant: Once constructed, this snapshot is strictly immutable.
 */
@Getter
@ToString
public class DatasetFreezeSnapshot {

    private final String datasetVersion;
    private final String freezeStatus; // FROZEN_SUCCESS, BLOCKED_BELOW_THRESHOLD, BLOCKED_BY_INTEGRITY
    private final Instant qualificationTimestamp;
    private final Instant frozenAt;
    private final long qualifyingSessionCount;
    private final long requiredThreshold;
    private final List<String> sourceEventIdentifiers;
    private final String featureSchemaVersion;
    private final String labelSchemaVersion;
    private final String activeRecommenderVersion;
    private final String fallbackRecommenderVersion;
    private final String eligibilityEngineVersion;
    private final String leakageAuditVersion;
    private final String integritySha256Hash;
    private final boolean immutable;
    private final boolean trainingEligible;
    private final boolean modelPromotionAllowed; // Always false
    private final List<TrainingExample> frozenExamples;
    private final DatasetQualificationSnapshot qualificationSnapshot;
    private final String message;

    @Builder
    public DatasetFreezeSnapshot(
            String datasetVersion,
            String freezeStatus,
            Instant qualificationTimestamp,
            Instant frozenAt,
            long qualifyingSessionCount,
            long requiredThreshold,
            List<String> sourceEventIdentifiers,
            String featureSchemaVersion,
            String labelSchemaVersion,
            String activeRecommenderVersion,
            String fallbackRecommenderVersion,
            String eligibilityEngineVersion,
            String leakageAuditVersion,
            String integritySha256Hash,
            boolean immutable,
            boolean trainingEligible,
            boolean modelPromotionAllowed,
            List<TrainingExample> frozenExamples,
            DatasetQualificationSnapshot qualificationSnapshot,
            String message
    ) {
        this.datasetVersion = datasetVersion != null ? datasetVersion : "NONE";
        this.freezeStatus = freezeStatus != null ? freezeStatus : "BLOCKED_BELOW_THRESHOLD";
        this.qualificationTimestamp = qualificationTimestamp;
        this.frozenAt = frozenAt != null ? frozenAt : Instant.now();
        this.qualifyingSessionCount = qualifyingSessionCount;
        this.requiredThreshold = requiredThreshold > 0 ? requiredThreshold : 100L;
        this.sourceEventIdentifiers = sourceEventIdentifiers != null
                ? Collections.unmodifiableList(sourceEventIdentifiers)
                : Collections.emptyList();
        this.featureSchemaVersion = featureSchemaVersion != null ? featureSchemaVersion : "1.0.0";
        this.labelSchemaVersion = labelSchemaVersion != null ? labelSchemaVersion : "1.0.0";
        this.activeRecommenderVersion = activeRecommenderVersion != null ? activeRecommenderVersion : "2.2.0-hybrid-semantic-384d";
        this.fallbackRecommenderVersion = fallbackRecommenderVersion != null ? fallbackRecommenderVersion : "1.0.0-deterministic";
        this.eligibilityEngineVersion = eligibilityEngineVersion != null ? eligibilityEngineVersion : "1.0.0-statutory";
        this.leakageAuditVersion = leakageAuditVersion != null ? leakageAuditVersion : "1.0.0-strict";
        this.integritySha256Hash = integritySha256Hash != null ? integritySha256Hash : "NONE";
        this.immutable = true;
        this.trainingEligible = trainingEligible;
        this.modelPromotionAllowed = false; // Always false
        this.frozenExamples = frozenExamples != null
                ? Collections.unmodifiableList(frozenExamples)
                : Collections.emptyList();
        this.qualificationSnapshot = qualificationSnapshot;
        this.message = message != null ? message : "";
    }
}
