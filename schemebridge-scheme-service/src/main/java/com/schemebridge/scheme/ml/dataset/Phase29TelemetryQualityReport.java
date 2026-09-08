package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Machine-readable telemetry quality metrics and classification report for Phase 29.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase29TelemetryQualityReport {

    private long totalRawEvents;
    private long validEvents;
    private long duplicateEvents;
    private long syntheticEvents;
    private long malformedEvents;
    private long piiViolations;
    private long orphanEvents;
    private long invalidSequenceEvents;

    private long validInteractionSessions;
    private long legitimateOutcomeSessions;
    private long usableTrainingPairs;

    private double syntheticExclusionRate;
    private double piiViolationRate;
    private double duplicateRate;
    private double malformedEventRate;
    private double sessionValidationRate;

    private Map<String, Long> relevanceGradesBreakdown;
    private Instant generatedAt;
}
