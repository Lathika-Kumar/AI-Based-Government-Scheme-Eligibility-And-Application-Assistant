package com.schemebridge.scheme.ml.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase34ModelEvaluationTest {

    @Mock
    private Phase34TrainingReadinessValidator readinessValidator;

    @Mock
    private Phase34ModelTrainingService trainingService;

    private Phase34ModelEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        evaluationService = new Phase34ModelEvaluationService(readinessValidator, trainingService);
    }

    @Test
    @DisplayName("evaluateLive returns INSUFFICIENT_DATA when outcomes < 100")
    void testEvaluateLive_InsufficientDataBelow100() {
        Phase34TrainingReadinessValidator.ReadinessValidationResult readiness =
                Phase34TrainingReadinessValidator.ReadinessValidationResult.builder()
                        .status("TRAINING_NOT_READY")
                        .trainingReady(false)
                        .modelTrainingAllowed(false)
                        .modelPromotionAllowed(false)
                        .legitimateOutcomeSessions(0L)
                        .requiredThreshold(100L)
                        .remainingOutcomeSessions(100L)
                        .build();

        when(readinessValidator.validateLive()).thenReturn(readiness);

        Phase34EvaluationReport report = evaluationService.evaluateLive();

        assertNotNull(report);
        assertEquals("INSUFFICIENT_DATA", report.getEvaluationStatus());
        assertEquals("2.2.0-hybrid-semantic-384d", report.getBaselineModel());
        assertEquals("1.0.0-deterministic", report.getFallbackModel());
        assertEquals("NONE", report.getCandidateModel());
        assertFalse(report.isModelPromotionAllowed());
        assertEquals("INSUFFICIENT_DATA", report.getComparisonResult());
    }

    @Test
    @DisplayName("evaluateCandidate evaluates against frozen test set and computes precision, recall, and f1")
    void testEvaluateCandidate_Success() {
        Phase34EvaluationReport report = evaluationService.evaluateCandidate(
                "candidate-model-v3",
                20L,
                8L,
                12L,
                0.89,
                0.84,
                0.86,
                0.87,
                "NDCG@5: 0.892 (+3.1%)",
                "CANDIDATE_IMPROVES"
        );

        assertNotNull(report);
        assertEquals("PASS", report.getEvaluationStatus());
        assertEquals("candidate-model-v3", report.getCandidateModel());
        assertEquals(20L, report.getTestSessionCount());
        assertEquals(8L, report.getPositiveOutcomeCount());
        assertEquals(12L, report.getNegativeOutcomeCount());
        assertEquals(0.89, report.getPrecision());
        assertEquals(0.84, report.getRecall());
        assertEquals(0.86, report.getF1());
        assertEquals("CANDIDATE_IMPROVES", report.getComparisonResult());
        assertEquals("2.2.0-hybrid-semantic-384d", report.getBaselineModel());
        assertFalse(report.isModelPromotionAllowed(), "Model promotion MUST remain blocked");
        assertEquals("PENDING_HUMAN_GOVERNANCE", report.getGovernanceStatus());
    }

    @Test
    @DisplayName("evaluateCandidate with 0 test sessions returns INSUFFICIENT_DATA")
    void testEvaluateCandidate_EmptyTestSet() {
        Phase34EvaluationReport report = evaluationService.evaluateCandidate(
                "candidate-model-v3",
                0L,
                0L,
                0L,
                null,
                null,
                null,
                null,
                "INSUFFICIENT_DATA",
                "INSUFFICIENT_DATA"
        );

        assertNotNull(report);
        assertEquals("INSUFFICIENT_DATA", report.getEvaluationStatus());
        assertEquals(0L, report.getTestSessionCount());
    }
}
