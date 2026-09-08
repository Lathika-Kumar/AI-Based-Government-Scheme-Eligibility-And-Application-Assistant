package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 31: Machine-readable telemetry quality and health report contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase31TelemetryQualityReport {

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

    private double validEventRate;
    private double rejectionRate;
    private double syntheticQuarantineRate;
    private double duplicateRate;
    private double piiViolationRate;
    private double malformedRate;

    private Map<String, Long> relevanceGradesBreakdown;
    private Instant generatedAt;
}
