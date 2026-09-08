package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Phase 34: Immutable Training Dataset Snapshot.
 * Represents a cryptographically versioned, frozen snapshot of genuine citizen outcome sessions
 * partitioned strictly at the session level into TRAIN, VALIDATION, and TEST sets.
 */
@Getter
@ToString
@Builder
public class TrainingDatasetSnapshot {

    private final String datasetVersion;
    private final Instant frozenAt;
    private final long totalSessions;
    private final long trainSessions;
    private final long validationSessions;
    private final long testSessions;
    private final long crossSplitLeakage;
    private final Map<String, Long> classDistribution;
    private final List<TrainingPair> pairs;
    private final long targetLeakageViolations;
    private final long piiViolations;
    private final long syntheticFixturesQuarantined;
    private final double statutoryEligibilityViolationRate;
}
