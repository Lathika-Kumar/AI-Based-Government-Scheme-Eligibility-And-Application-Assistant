package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Phase 34: Training Handoff Contract DTO.
 * Matches Objective J & K machine-readable specification.
 */
@Getter
@ToString
@Builder
public class Phase34TrainingHandoff {

    private final String phase;
    private final String status; // TRAINING_NOT_READY, TRAINING_READY
    private final boolean trainingEligible;
    private final boolean trainingReady;
    private final long legitimateOutcomeSessions;
    private final long requiredOutcomeSessions;
    private final long threshold;
    private final long remainingSessions;
    private final boolean modelTrainingAllowed;
    private final boolean modelPromotionAllowed; // Always false
    private final String activeModel;
    private final String fallbackModel;
    private final int circuitBreakerMs;
    private final long syntheticEventsGenerated;
    private final long syntheticFixturesQuarantined;
    private final long piiViolations;
    private final long targetLeakage;
    private final long sessionSplitLeakage;
    private final boolean humanGovernanceApprovalRequired;
    private final Map<String, Integer> databaseMutations;
    private final String message;
    private final String evaluatedAt;
    private final Instant generatedAt;
}

