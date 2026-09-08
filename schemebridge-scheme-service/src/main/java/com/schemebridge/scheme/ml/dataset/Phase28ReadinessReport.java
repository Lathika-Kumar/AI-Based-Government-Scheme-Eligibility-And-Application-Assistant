package com.schemebridge.scheme.ml.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Deterministic production telemetry readiness and data quality report contract for Phase 28.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Phase28ReadinessReport {

    private TrainingReadinessStatus trainingStatus;
    private long legitimateOutcomeSessions;
    private long requiredOutcomeSessions;
    private long remainingOutcomeSessions;
    private long syntheticFixturesQuarantined;
    private long piiViolations;
    private long statutoryEligibilityViolations;

    private String activeModel;
    private String fallbackModel;
    private int circuitBreakerMs;

    // Telemetry Quality Metrics
    private long totalRawRecommendationEvents;
    private long totalRawApplicationEvents;
    private long legitimateEvents;
    private long syntheticEvents;

    private Map<String, Long> relevanceGradesBreakdown;
    private long uniqueSessions;
    private long uniqueSchemes;
    private long invalidMalformedEvents;

    private Instant generatedAt;
}
