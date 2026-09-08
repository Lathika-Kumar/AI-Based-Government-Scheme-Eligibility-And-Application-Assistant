package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Phase 38: Behavioral Dataset Qualification Snapshot.
 * Captures comprehensive dataset qualification metrics prior to permitting any offline training run.
 * Invariant: modelPromotionAllowed is permanently false.
 */
@Getter
@ToString
@Builder
public class DatasetQualificationSnapshot {

    private final String status; // NOT_READY, ELIGIBLE_FOR_OFFLINE_TRAINING, DISQUALIFIED_BY_INTEGRITY
    private final long totalEvents;
    private final long validEvents;
    private final long syntheticEvents;
    private final long piiViolations;
    private final long orphanEvents;
    private final long duplicateEvents;
    private final long invalidSequenceEvents;
    private final long legitimateOutcomeSessions;
    private final long eligibleTrainingExamples;
    private final long rejectedTrainingExamples;
    private final long targetLeakageViolations;
    private final long minimumOutcomeThreshold;
    private final long remainingOutcomes;
    private final boolean trainingReady;
    private final boolean modelTrainingAllowed;
    private final boolean modelPromotionAllowed;
    private final double statutoryEligibilityViolationRate;
    private final String activeModel;
    private final String fallbackModel;
    private final int circuitBreakerMs;
    private final String qualificationMessage;
    private final Instant evaluatedAt;
    private final List<String> qualificationErrors;
    private final Map<String, Long> relevanceGradesDistribution;
}
