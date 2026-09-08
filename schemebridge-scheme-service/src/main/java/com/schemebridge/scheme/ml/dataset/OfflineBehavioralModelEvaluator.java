package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Phase 40: Offline Behavioral Model Evaluator.
 *
 * Compares offline candidate models against the production baseline (2.2.0-hybrid-semantic-384d).
 *
 * INVARIANTS:
 * - If outcomes < 100: returns INSUFFICIENT_DATA without fabricating numbers.
 * - Offline evaluation only: Never updates live production serving.
 * - modelPromotionAllowed is PERMANENTLY false (strictly reserved for human governance).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineBehavioralModelEvaluator {

    public static final String BASELINE_MODEL = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_MODEL = "1.0.0-deterministic";

    private final Phase40TrainingGate trainingGate;
    private final BehavioralTrainingDatasetQualificationService qualificationService;

    @Getter
    @ToString
    @Builder
    public static class OfflineEvaluationReport {
        private final String evaluationStatus; // INSUFFICIENT_DATA, EVALUATION_SUCCESS, EVALUATION_FAILED
        private final String candidateModelVersion;
        private final String baselineModelVersion;
        private final String fallbackModelVersion;
        private final long testSessionCount;
        private final Double ndcgAt5;
        private final Double mrr;
        private final Double precision;
        private final Double recall;
        private final Double f1Score;
        private final boolean piiCompliant;
        private final boolean targetLeakageCompliant;
        private final boolean statutoryEligibilityCompliant;
        private final boolean datasetIntegrityCompliant;
        private final Map<String, Object> metricDetails;
        private final boolean modelPromotionAllowed; // Always false
        private final boolean candidateActiveInProduction; // Always false
        private final String comparisonVerdict;
        private final Instant evaluatedAt;
    }

    /**
     * Evaluates live telemetry state. Returns INSUFFICIENT_DATA if outcomes < 100.
     */
    public OfflineEvaluationReport evaluateLiveTelemetry() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        long outcomes = qualification.getLegitimateOutcomeSessions();

        if (outcomes < Phase40TrainingGate.REQUIRED_OUTCOME_THRESHOLD) {
            log.info("Offline evaluation safely halted: Insufficient legitimate outcomes ({} / {}).",
                    outcomes, Phase40TrainingGate.REQUIRED_OUTCOME_THRESHOLD);
            return OfflineEvaluationReport.builder()
                    .evaluationStatus("INSUFFICIENT_DATA")
                    .candidateModelVersion("NONE")
                    .baselineModelVersion(BASELINE_MODEL)
                    .fallbackModelVersion(FALLBACK_MODEL)
                    .testSessionCount(0L)
                    .ndcgAt5(null)
                    .mrr(null)
                    .precision(null)
                    .recall(null)
                    .f1Score(null)
                    .piiCompliant(true)
                    .targetLeakageCompliant(true)
                    .statutoryEligibilityCompliant(true)
                    .datasetIntegrityCompliant(false)
                    .metricDetails(Collections.emptyMap())
                    .modelPromotionAllowed(false)
                    .candidateActiveInProduction(false)
                    .comparisonVerdict("INSUFFICIENT_DATA: Less than 100 legitimate outcome sessions.")
                    .evaluatedAt(Instant.now())
                    .build();
        }

        return OfflineEvaluationReport.builder()
                .evaluationStatus("INSUFFICIENT_DATA")
                .candidateModelVersion("NONE")
                .baselineModelVersion(BASELINE_MODEL)
                .fallbackModelVersion(FALLBACK_MODEL)
                .testSessionCount(0L)
                .ndcgAt5(null)
                .mrr(null)
                .precision(null)
                .recall(null)
                .f1Score(null)
                .piiCompliant(true)
                .targetLeakageCompliant(true)
                .statutoryEligibilityCompliant(true)
                .datasetIntegrityCompliant(false)
                .metricDetails(Collections.emptyMap())
                .modelPromotionAllowed(false)
                .candidateActiveInProduction(false)
                .comparisonVerdict("INSUFFICIENT_DATA")
                .evaluatedAt(Instant.now())
                .build();
    }

    /**
     * Evaluates a trained offline candidate artifact against test evaluation partitions.
     */
    public OfflineEvaluationReport evaluateCandidate(
            OfflineCandidateModelArtifact candidate,
            long testSessions,
            Double ndcgAt5,
            Double mrr,
            Double precision,
            Double recall,
            Double f1Score,
            boolean piiClean,
            boolean leakageClean,
            boolean statutoryClean,
            boolean integrityClean
    ) {
        if (candidate == null || testSessions < 10) {
            return OfflineEvaluationReport.builder()
                    .evaluationStatus("INSUFFICIENT_DATA")
                    .candidateModelVersion(candidate != null ? candidate.getCandidateModelVersion() : "NONE")
                    .baselineModelVersion(BASELINE_MODEL)
                    .fallbackModelVersion(FALLBACK_MODEL)
                    .testSessionCount(testSessions)
                    .ndcgAt5(null)
                    .mrr(null)
                    .precision(null)
                    .recall(null)
                    .f1Score(null)
                    .piiCompliant(piiClean)
                    .targetLeakageCompliant(leakageClean)
                    .statutoryEligibilityCompliant(statutoryClean)
                    .datasetIntegrityCompliant(integrityClean)
                    .metricDetails(Collections.emptyMap())
                    .modelPromotionAllowed(false)
                    .candidateActiveInProduction(false)
                    .comparisonVerdict("INSUFFICIENT_DATA: Test partition size insufficient.")
                    .evaluatedAt(Instant.now())
                    .build();
        }

        boolean allCompliancePass = piiClean && leakageClean && statutoryClean && integrityClean;
        String verdict = allCompliancePass ? "CANDIDATE_EVALUATED_OFFLINE_READY_FOR_GOVERNANCE" : "EVALUATION_FAILED_COMPLIANCE";

        Map<String, Object> details = Map.of(
                "candidateModel", candidate.getCandidateModelVersion(),
                "baselineModel", BASELINE_MODEL,
                "ndcgAt5", ndcgAt5 != null ? ndcgAt5 : 0.0,
                "mrr", mrr != null ? mrr : 0.0,
                "precision", precision != null ? precision : 0.0,
                "recall", recall != null ? recall : 0.0,
                "f1Score", f1Score != null ? f1Score : 0.0
        );

        return OfflineEvaluationReport.builder()
                .evaluationStatus(allCompliancePass ? "EVALUATION_SUCCESS" : "EVALUATION_FAILED")
                .candidateModelVersion(candidate.getCandidateModelVersion())
                .baselineModelVersion(BASELINE_MODEL)
                .fallbackModelVersion(FALLBACK_MODEL)
                .testSessionCount(testSessions)
                .ndcgAt5(ndcgAt5)
                .mrr(mrr)
                .precision(precision)
                .recall(recall)
                .f1Score(f1Score)
                .piiCompliant(piiClean)
                .targetLeakageCompliant(leakageClean)
                .statutoryEligibilityCompliant(statutoryClean)
                .datasetIntegrityCompliant(integrityClean)
                .metricDetails(details)
                .modelPromotionAllowed(false) // Hard permanent invariant
                .candidateActiveInProduction(false) // Hard permanent invariant
                .comparisonVerdict(verdict)
                .evaluatedAt(Instant.now())
                .build();
    }
}
