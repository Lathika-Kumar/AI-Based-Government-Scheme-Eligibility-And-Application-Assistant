package com.schemebridge.scheme.ml.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase34PromotionFirewallTest {

    @Mock
    private Phase31ContinuousReadinessMonitor readinessMonitor;

    @Mock
    private Phase34TrainingReadinessValidator readinessValidator;

    @Mock
    private TrainingDatasetIntegrityValidator integrityValidator;

    @Mock
    private TargetLeakageValidator targetLeakageValidator;

    @Mock
    private SessionAttributionService sessionAttributionService;

    private Phase34ModelTrainingService trainingService;
    private Phase34ModelEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        trainingService = new Phase34ModelTrainingService(
                readinessValidator,
                integrityValidator,
                targetLeakageValidator,
                sessionAttributionService,
                readinessMonitor
        );
        evaluationService = new Phase34ModelEvaluationService(readinessValidator, trainingService);
    }

    @Test
    @DisplayName("Promotion firewall: modelPromotionAllowed is FALSE across all readiness states")
    void testReadinessHandoff_PromotionAlwaysBlocked() {
        TrainingReadinessGate gate = new TrainingReadinessGate();
        Phase34TrainingReadinessValidator validator = new Phase34TrainingReadinessValidator(readinessMonitor, gate);

        Phase31ReadinessReport report1 = Phase31ReadinessReport.builder()
                .status("TRAINING_NOT_READY")
                .legitimateOutcomeSessions(50L)
                .requiredThreshold(100L)
                .remainingOutcomeSessions(50L)
                .trainingReady(false)
                .modelTrainingAllowed(false)
                .modelPromotionAllowed(false)
                .syntheticFixturesQuarantined(29L)
                .piiViolations(0L)
                .activeModel("2.2.0-hybrid-semantic-384d")
                .fallbackModel("1.0.0-deterministic")
                .build();

        when(readinessMonitor.evaluateLiveTelemetry()).thenReturn(
                Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot.builder()
                        .legitimateOutcomeSessions(50L)
                        .readinessReport(report1)
                        .build()
        );
        Phase34TrainingHandoff handoff1 = validator.buildHandoff();
        assertFalse(handoff1.isModelPromotionAllowed(), "Promotion must be false below 100");

        Phase31ReadinessReport report2 = Phase31ReadinessReport.builder()
                .status("TRAINING_READY")
                .legitimateOutcomeSessions(100L)
                .requiredThreshold(100L)
                .remainingOutcomeSessions(0L)
                .trainingReady(true)
                .modelTrainingAllowed(true)
                .modelPromotionAllowed(false)
                .syntheticFixturesQuarantined(29L)
                .piiViolations(0L)
                .activeModel("2.2.0-hybrid-semantic-384d")
                .fallbackModel("1.0.0-deterministic")
                .build();

        when(readinessMonitor.evaluateLiveTelemetry()).thenReturn(
                Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot.builder()
                        .legitimateOutcomeSessions(100L)
                        .readinessReport(report2)
                        .build()
        );
        Phase34TrainingHandoff handoff2 = validator.buildHandoff();
        assertFalse(handoff2.isModelPromotionAllowed(), "Promotion must be false at 100");
    }

    @Test
    @DisplayName("Promotion firewall: modelPromotionAllowed is FALSE even after successful candidate training")
    void testTrainingReport_PromotionAlwaysBlocked() {
        TrainingDatasetSnapshot snapshot = TrainingDatasetSnapshot.builder()
                .datasetVersion("3.4.0-snapshot-test")
                .frozenAt(Instant.now())
                .totalSessions(100L)
                .trainSessions(70L)
                .validationSessions(15L)
                .testSessions(15L)
                .crossSplitLeakage(0L)
                .targetLeakageViolations(0L)
                .piiViolations(0L)
                .statutoryEligibilityViolationRate(0.0)
                .classDistribution(Map.of("GRADE_3_CONVERTED", 25L, "GRADE_1_VIEWED", 75L))
                .build();

        when(integrityValidator.validate(eq(100L), anyLong(), anyLong(), eq(0L), anyLong(), anyLong(), anyLong(), eq(0L), eq(0L), eq(0.0), any()))
                .thenReturn(TrainingDatasetIntegrityValidator.IntegrityValidationReport.builder()
                        .status(TrainingDatasetIntegrityValidator.IntegrityStatus.PASS)
                        .passed(true)
                        .legitimateOutcomeCount(100L)
                        .build());

        Phase34TrainingReport report = trainingService.executeTrainingFromSnapshot(snapshot);

        assertTrue(report.isModelTrainingAllowed());
        assertEquals("candidate-model-v3", report.getCandidateModelVersion());
        assertFalse(report.isModelPromotionAllowed(), "Training MUST NOT set modelPromotionAllowed to true");
        assertEquals("PENDING_HUMAN_GOVERNANCE", report.getGovernanceState());
        assertEquals("2.2.0-hybrid-semantic-384d", report.getActiveModel());
    }

    @Test
    @DisplayName("Promotion firewall: modelPromotionAllowed is FALSE even after passing candidate evaluation")
    void testEvaluationReport_PromotionAlwaysBlocked() {
        Phase34EvaluationReport eval = evaluationService.evaluateCandidate(
                "candidate-model-v3",
                15L,
                5L,
                10L,
                0.91,
                0.85,
                0.88,
                0.89,
                "NDCG@5: 0.901",
                "CANDIDATE_IMPROVES"
        );

        assertEquals("PASS", eval.getEvaluationStatus());
        assertFalse(eval.isModelPromotionAllowed(), "Evaluation MUST NOT set modelPromotionAllowed to true");
        assertEquals("PENDING_HUMAN_GOVERNANCE", eval.getGovernanceStatus());
        assertEquals("2.2.0-hybrid-semantic-384d", eval.getBaselineModel());
    }
}
