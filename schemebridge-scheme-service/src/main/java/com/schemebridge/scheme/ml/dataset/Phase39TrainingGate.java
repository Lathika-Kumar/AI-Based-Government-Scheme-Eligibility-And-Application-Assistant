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
 * Phase 39: Strict Behavioral ML Training & Promotion Firewall Gate.
 * Enforces the comprehensive set of pre-training criteria before offline training is permitted.
 *
 * CRITICAL NON-NEGOTIABLE INVARIANTS:
 * - When legitimate outcomes < 100: Training is strictly prohibited.
 * - Under current database state (0 / 100): Training remains locked (TRAINING_NOT_READY).
 * - Even if outcomes reach 100+: Model promotion remains PERMANENTLY false (strictly governed by human deployment authority).
 * - Zero automatic training. Zero automatic promotion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Phase39TrainingGate {

    public static final long OUTCOME_THRESHOLD = 100L;
    public static final String ACTIVE_MODEL = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL = "1.0.0-deterministic";

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
        private final long syntheticContamination;
        private final long piiViolations;
        private final long targetLeakageViolations;
        private final String datasetFreezeStatus;
        private final String integrityHash;
        private final String message;
    }

    /**
     * Evaluates live database telemetry and freeze state against all training firewall requirements.
     */
    public GateDecision evaluateLive() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        DatasetFreezeSnapshot freezeSnapshot = datasetFreezeService.freezeLiveDataset();
        return evaluateInternal(qualification, freezeSnapshot);
    }

    /**
     * Evaluates explicit or in-memory events against all training firewall requirements (used for testing).
     */
    public GateDecision evaluate(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        DatasetQualificationSnapshot qualification = qualificationService.qualify(recEvents, appEvents);
        DatasetFreezeSnapshot freezeSnapshot = datasetFreezeService.freeze(recEvents, appEvents);
        return evaluateInternal(qualification, freezeSnapshot);
    }

    private GateDecision evaluateInternal(DatasetQualificationSnapshot qualification, DatasetFreezeSnapshot freezeSnapshot) {
        long outcomes = qualification.getLegitimateOutcomeSessions();
        long remaining = Math.max(0L, OUTCOME_THRESHOLD - outcomes);
        long pii = qualification.getPiiViolations();
        long leakage = qualification.getTargetLeakageViolations();
        long synthetic = qualification.getSyntheticEvents();

        // 1. Threshold check
        if (outcomes < OUTCOME_THRESHOLD) {
            String message = String.format("TRAINING_NOT_READY: Legitimate outcomes (%d / %d, remaining: %d) below threshold. Behavioral ML training locked.",
                    outcomes, OUTCOME_THRESHOLD, remaining);
            return GateDecision.builder()
                    .gateStatus("TRAINING_BLOCKED_BELOW_THRESHOLD")
                    .trainingPermitted(false)
                    .modelPromotionAllowed(false)
                    .legitimateOutcomeSessions(outcomes)
                    .requiredThreshold(OUTCOME_THRESHOLD)
                    .remainingSessions(remaining)
                    .syntheticContamination(synthetic)
                    .piiViolations(pii)
                    .targetLeakageViolations(leakage)
                    .datasetFreezeStatus(freezeSnapshot.getFreezeStatus())
                    .integrityHash(freezeSnapshot.getIntegritySha256Hash())
                    .message(message)
                    .build();
        }

        // 2. Pre-training integrity check
        boolean freezeOk = "FROZEN_SUCCESS".equals(freezeSnapshot.getFreezeStatus())
                && !"NONE".equals(freezeSnapshot.getIntegritySha256Hash());
        boolean integrityClean = (pii == 0) && (leakage == 0) && (synthetic == 0);

        if (!freezeOk || !integrityClean) {
            String message = String.format("TRAINING_BLOCKED_BY_INTEGRITY: Threshold met (%d >= %d) but integrity or freeze failed (PII: %d, Leakage: %d, Synthetic: %d, Freeze: %s).",
                    outcomes, OUTCOME_THRESHOLD, pii, leakage, synthetic, freezeSnapshot.getFreezeStatus());
            return GateDecision.builder()
                    .gateStatus("TRAINING_BLOCKED_BY_INTEGRITY")
                    .trainingPermitted(false)
                    .modelPromotionAllowed(false)
                    .legitimateOutcomeSessions(outcomes)
                    .requiredThreshold(OUTCOME_THRESHOLD)
                    .remainingSessions(0L)
                    .syntheticContamination(synthetic)
                    .piiViolations(pii)
                    .targetLeakageViolations(leakage)
                    .datasetFreezeStatus(freezeSnapshot.getFreezeStatus())
                    .integrityHash(freezeSnapshot.getIntegritySha256Hash())
                    .message(message)
                    .build();
        }

        // 3. Training permitted for offline sandbox only; promotion remains blocked
        String message = String.format("TRAINING_PERMITTED_OFFLINE_ONLY: All pre-training criteria satisfied (%d outcomes, hash: %s). Offline training unlocked. Promotion requires human governance.",
                outcomes, freezeSnapshot.getIntegritySha256Hash());
        return GateDecision.builder()
                .gateStatus("TRAINING_PERMITTED_OFFLINE_ONLY")
                .trainingPermitted(true)
                .modelPromotionAllowed(false) // Permanently false
                .legitimateOutcomeSessions(outcomes)
                .requiredThreshold(OUTCOME_THRESHOLD)
                .remainingSessions(0L)
                .syntheticContamination(0L)
                .piiViolations(0L)
                .targetLeakageViolations(0L)
                .datasetFreezeStatus(freezeSnapshot.getFreezeStatus())
                .integrityHash(freezeSnapshot.getIntegritySha256Hash())
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
