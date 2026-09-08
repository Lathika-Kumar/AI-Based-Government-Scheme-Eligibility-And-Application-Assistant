package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 40: Authoritative Pre-Training Gate & Production Promotion Firewall.
 *
 * Enforces all eight mandatory pre-conditions before offline behavioral training can execute:
 * - Gate A: legitimateOutcomeSessions >= 100
 * - Gate B: datasetFreezeSuccessful == true
 * - Gate C: piiViolations == 0
 * - Gate D: targetLeakageViolations == 0
 * - Gate E: syntheticContamination == 0
 * - Gate F: allTrainingExamplesValid == true
 * - Gate G: chronologicalSequencesValid == true
 * - Gate H: recommendationAnchorsValid == true
 *
 * CRITICAL ARCHITECTURAL REALITY:
 * Under current database baseline (0 / 100), the status is strictly TRAINING_BLOCKED_BELOW_THRESHOLD.
 * modelTrainingAllowed = false, modelPromotionAllowed = false.
 * Even if all gates pass in future, modelPromotionAllowed remains permanently false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Phase40TrainingGate {

    public static final long REQUIRED_OUTCOME_THRESHOLD = 100L;
    public static final String ACTIVE_MODEL_VERSION = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL_VERSION = "1.0.0-deterministic";

    private final BehavioralTrainingDatasetQualificationService qualificationService;
    private final DatasetFreezeService datasetFreezeService;

    @Getter
    @ToString
    @Builder
    public static class GateDecision {
        private final String gateStatus; // TRAINING_BLOCKED_BELOW_THRESHOLD, TRAINING_BLOCKED_BY_INTEGRITY, TRAINING_PERMITTED_OFFLINE_ONLY
        private final boolean trainingPermitted;
        private final boolean modelPromotionAllowed; // Always false
        private final long legitimateOutcomeSessions;
        private final long requiredThreshold;
        private final long remainingSessions;
        private final boolean datasetFreezeSuccessful;
        private final long piiViolations;
        private final long targetLeakageViolations;
        private final long syntheticContamination;
        private final boolean allTrainingExamplesValid;
        private final boolean chronologicalSequencesValid;
        private final boolean recommendationAnchorsValid;
        private final String datasetVersion;
        private final String datasetSha256Digest;
        private final String message;
    }

    /**
     * Evaluates live database state against all 8 gates.
     */
    public GateDecision evaluateLive() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        DatasetFreezeSnapshot freezeSnapshot = datasetFreezeService.freezeLiveDataset();
        return evaluateInternal(qualification, freezeSnapshot);
    }

    /**
     * Evaluates a provided freeze snapshot against all 8 gates.
     */
    public GateDecision evaluate(DatasetFreezeSnapshot freezeSnapshot) {
        if (freezeSnapshot == null) {
            return buildBlockedThresholdDecision(0, 0, 0, 0, false, "DatasetFreezeSnapshot is null");
        }
        DatasetQualificationSnapshot qualification = freezeSnapshot.getQualificationSnapshot();
        if (qualification == null) {
            qualification = qualificationService.qualifyLive();
        }
        return evaluateInternal(qualification, freezeSnapshot);
    }

    /**
     * Evaluates explicit telemetry event streams (used for unit test verification).
     */
    public GateDecision evaluate(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        DatasetQualificationSnapshot qualification = qualificationService.qualify(recEvents, appEvents);
        DatasetFreezeSnapshot freezeSnapshot = datasetFreezeService.freeze(recEvents, appEvents);
        return evaluateInternal(qualification, freezeSnapshot);
    }

    private GateDecision evaluateInternal(DatasetQualificationSnapshot qualification, DatasetFreezeSnapshot freezeSnapshot) {
        long outcomes = qualification != null ? qualification.getLegitimateOutcomeSessions() : 0L;
        long remaining = Math.max(0L, REQUIRED_OUTCOME_THRESHOLD - outcomes);
        long pii = qualification != null ? qualification.getPiiViolations() : 0L;
        long leakage = qualification != null ? qualification.getTargetLeakageViolations() : 0L;
        long synthetic = qualification != null ? qualification.getSyntheticEvents() : 0L;

        boolean freezeSuccess = freezeSnapshot != null
                && "FROZEN_SUCCESS".equals(freezeSnapshot.getFreezeStatus())
                && freezeSnapshot.getIntegritySha256Hash() != null
                && !"NONE".equals(freezeSnapshot.getIntegritySha256Hash())
                && freezeSnapshot.getIntegritySha256Hash().length() == 64;

        boolean sequencesValid = qualification != null && qualification.getInvalidSequenceEvents() == 0;
        boolean anchorsValid = qualification != null && qualification.getOrphanEvents() == 0;
        boolean examplesValid = qualification != null && qualification.getRejectedTrainingExamples() == 0;

        // Gate A: Outcome threshold check (< 100)
        if (outcomes < REQUIRED_OUTCOME_THRESHOLD) {
            return buildBlockedThresholdDecision(outcomes, pii, leakage, synthetic, freezeSuccess,
                    String.format("TRAINING_BLOCKED_BELOW_THRESHOLD: Legitimate outcomes (%d / %d, remaining: %d) below threshold. Behavioral ML training locked.",
                            outcomes, REQUIRED_OUTCOME_THRESHOLD, remaining));
        }

        // Gates B - H: Integrity and qualification checks
        boolean integrityClean = (pii == 0) && (leakage == 0) && (synthetic == 0);
        boolean structureValid = freezeSuccess && sequencesValid && anchorsValid && examplesValid;

        if (!integrityClean || !structureValid) {
            String message = String.format("TRAINING_BLOCKED_BY_INTEGRITY: Threshold met (%d >= %d) but integrity or verification gates failed: " +
                            "[freezeSuccess=%b, pii=%d, leakage=%d, synthetic=%d, sequencesValid=%b, anchorsValid=%b, examplesValid=%b]",
                    outcomes, REQUIRED_OUTCOME_THRESHOLD, freezeSuccess, pii, leakage, synthetic, sequencesValid, anchorsValid, examplesValid);
            log.warn("Phase 40 Training Gate: {}", message);

            return GateDecision.builder()
                    .gateStatus("TRAINING_BLOCKED_BY_INTEGRITY")
                    .trainingPermitted(false)
                    .modelPromotionAllowed(false)
                    .legitimateOutcomeSessions(outcomes)
                    .requiredThreshold(REQUIRED_OUTCOME_THRESHOLD)
                    .remainingSessions(0L)
                    .datasetFreezeSuccessful(freezeSuccess)
                    .piiViolations(pii)
                    .targetLeakageViolations(leakage)
                    .syntheticContamination(synthetic)
                    .allTrainingExamplesValid(examplesValid)
                    .chronologicalSequencesValid(sequencesValid)
                    .recommendationAnchorsValid(anchorsValid)
                    .datasetVersion(freezeSnapshot != null ? freezeSnapshot.getDatasetVersion() : "NONE")
                    .datasetSha256Digest(freezeSnapshot != null ? freezeSnapshot.getIntegritySha256Hash() : "NONE")
                    .message(message)
                    .build();
        }

        // All 8 gates passed -> Offline sandbox training unlocked (promotion remains permanently false)
        String message = String.format("TRAINING_PERMITTED_OFFLINE_ONLY: All 8 pre-training gates satisfied (%d outcomes, hash: %s). Offline candidate training allowed. Promotion requires human deployment authority.",
                outcomes, freezeSnapshot.getIntegritySha256Hash());
        log.info("Phase 40 Training Gate: {}", message);

        return GateDecision.builder()
                .gateStatus("TRAINING_PERMITTED_OFFLINE_ONLY")
                .trainingPermitted(true)
                .modelPromotionAllowed(false) // Hard permanent invariant
                .legitimateOutcomeSessions(outcomes)
                .requiredThreshold(REQUIRED_OUTCOME_THRESHOLD)
                .remainingSessions(0L)
                .datasetFreezeSuccessful(true)
                .piiViolations(0L)
                .targetLeakageViolations(0L)
                .syntheticContamination(0L)
                .allTrainingExamplesValid(true)
                .chronologicalSequencesValid(true)
                .recommendationAnchorsValid(true)
                .datasetVersion(freezeSnapshot.getDatasetVersion())
                .datasetSha256Digest(freezeSnapshot.getIntegritySha256Hash())
                .message(message)
                .build();
    }

    private GateDecision buildBlockedThresholdDecision(long outcomes, long pii, long leakage, long synthetic, boolean freezeSuccess, String message) {
        long remaining = Math.max(0L, REQUIRED_OUTCOME_THRESHOLD - outcomes);
        return GateDecision.builder()
                .gateStatus("TRAINING_BLOCKED_BELOW_THRESHOLD")
                .trainingPermitted(false)
                .modelPromotionAllowed(false)
                .legitimateOutcomeSessions(outcomes)
                .requiredThreshold(REQUIRED_OUTCOME_THRESHOLD)
                .remainingSessions(remaining)
                .datasetFreezeSuccessful(freezeSuccess)
                .piiViolations(pii)
                .targetLeakageViolations(leakage)
                .syntheticContamination(synthetic)
                .allTrainingExamplesValid(false)
                .chronologicalSequencesValid(true)
                .recommendationAnchorsValid(true)
                .datasetVersion("NONE")
                .datasetSha256Digest("NONE")
                .message(message)
                .build();
    }

    /**
     * Enforces the training gate. Throws IllegalStateException if training is invoked while blocked.
     */
    public void enforceTrainingGate() {
        GateDecision decision = evaluateLive();
        if (!decision.isTrainingPermitted()) {
            throw new IllegalStateException("CRITICAL FIREWALL: Behavioral ML training attempt rejected: " + decision.getMessage());
        }
    }
}
