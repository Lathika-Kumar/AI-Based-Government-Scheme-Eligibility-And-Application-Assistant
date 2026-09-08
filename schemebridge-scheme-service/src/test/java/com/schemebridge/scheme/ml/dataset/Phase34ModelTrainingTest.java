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
class Phase34ModelTrainingTest {

    @Mock
    private Phase34TrainingReadinessValidator readinessValidator;

    @Mock
    private TrainingDatasetIntegrityValidator integrityValidator;

    @Mock
    private TargetLeakageValidator targetLeakageValidator;

    @Mock
    private SessionAttributionService sessionAttributionService;

    @Mock
    private Phase31ContinuousReadinessMonitor readinessMonitor;

    private Phase34ModelTrainingService trainingService;

    @BeforeEach
    void setUp() {
        trainingService = new Phase34ModelTrainingService(
                readinessValidator,
                integrityValidator,
                targetLeakageValidator,
                sessionAttributionService,
                readinessMonitor
        );
    }

    @Test
    @DisplayName("Live execution returns BLOCKED_BY_READINESS when outcomes < 100")
    void testLiveExecution_BlockedBelow100() {
        Phase34TrainingReadinessValidator.ReadinessValidationResult readiness =
                Phase34TrainingReadinessValidator.ReadinessValidationResult.builder()
                        .status("TRAINING_NOT_READY")
                        .trainingReady(false)
                        .modelTrainingAllowed(false)
                        .modelPromotionAllowed(false)
                        .legitimateOutcomeSessions(0L)
                        .requiredThreshold(100L)
                        .remainingOutcomeSessions(100L)
                        .message("Insufficient legitimate outcome sessions: 0 / 100")
                        .build();

        when(readinessValidator.validateLive()).thenReturn(readiness);

        Phase34TrainingReport report = trainingService.executeLiveTraining();

        assertNotNull(report);
        assertEquals("BLOCKED_BY_READINESS", report.getTrainingStatus());
        assertEquals("NONE", report.getCandidateModelVersion());
        assertEquals("2.2.0-hybrid-semantic-384d", report.getActiveModel());
        assertEquals("1.0.0-deterministic", report.getFallbackModel());
        assertFalse(report.isModelTrainingAllowed());
        assertFalse(report.isModelPromotionAllowed());
        assertEquals("NONE", report.getGovernanceState());
    }

    @Test
    @DisplayName("Training from snapshot builds candidate candidate-model-v3 and sets PENDING_HUMAN_GOVERNANCE")
    void testTrainFromSnapshot_Success() {
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

        TrainingDatasetIntegrityValidator.IntegrityValidationReport integrity =
                TrainingDatasetIntegrityValidator.IntegrityValidationReport.builder()
                        .status(TrainingDatasetIntegrityValidator.IntegrityStatus.PASS)
                        .passed(true)
                        .legitimateOutcomeCount(100L)
                        .build();

        when(integrityValidator.validate(eq(100L), anyLong(), anyLong(), eq(0L), anyLong(), anyLong(), anyLong(), eq(0L), eq(0L), eq(0.0), any()))
                .thenReturn(integrity);

        Phase34TrainingReport report = trainingService.executeTrainingFromSnapshot(snapshot);

        assertNotNull(report);
        assertEquals("TRAINED", report.getTrainingStatus());
        assertEquals("candidate-model-v3", report.getCandidateModelVersion());
        assertEquals("2.2.0-hybrid-semantic-384d", report.getActiveModel(), "Active model MUST remain untouched");
        assertEquals("1.0.0-deterministic", report.getFallbackModel());
        assertTrue(report.isModelTrainingAllowed());
        assertFalse(report.isModelPromotionAllowed(), "Promotion MUST remain blocked");
        assertEquals("PENDING_HUMAN_GOVERNANCE", report.getGovernanceState());
    }

    @Test
    @DisplayName("Training fails if snapshot has cross-split session leakage")
    void testTrainFromSnapshot_CrossSplitLeakageFails() {
        TrainingDatasetSnapshot snapshot = TrainingDatasetSnapshot.builder()
                .datasetVersion("3.4.0-snapshot-test")
                .frozenAt(Instant.now())
                .totalSessions(100L)
                .crossSplitLeakage(2L)
                .build();

        TrainingDatasetIntegrityValidator.IntegrityValidationReport integrity =
                TrainingDatasetIntegrityValidator.IntegrityValidationReport.builder()
                        .status(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL)
                        .passed(false)
                        .errorMessages(java.util.List.of("Cross-split session leakage detected: 2"))
                        .build();

        when(integrityValidator.validate(eq(100L), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), eq(2L), anyLong(), anyDouble(), any()))
                .thenReturn(integrity);

        Phase34TrainingReport report = trainingService.executeTrainingFromSnapshot(snapshot);

        assertEquals("BLOCKED_BY_INTEGRITY", report.getTrainingStatus());
        assertEquals("NONE", report.getCandidateModelVersion());
        assertFalse(report.isModelPromotionAllowed());
    }
}
