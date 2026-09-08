package com.schemebridge.scheme.ml.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase34TrainingReadinessValidationTest {

    @Mock
    private Phase31ContinuousReadinessMonitor readinessMonitor;

    private TrainingReadinessGate trainingReadinessGate;
    private Phase34TrainingReadinessValidator readinessValidator;

    @BeforeEach
    void setUp() {
        trainingReadinessGate = new TrainingReadinessGate();
        readinessValidator = new Phase34TrainingReadinessValidator(readinessMonitor, trainingReadinessGate);
    }

    @Test
    @DisplayName("0-99 outcomes -> TRAINING_NOT_READY, trainingReady=false, modelTrainingAllowed=false, modelPromotionAllowed=false")
    void testBelowThreshold_Blocked() {
        Phase31ReadinessReport report = Phase31ReadinessReport.builder()
                .status("TRAINING_NOT_READY")
                .legitimateOutcomeSessions(0L)
                .requiredThreshold(100L)
                .remainingOutcomeSessions(100L)
                .trainingReady(false)
                .modelTrainingAllowed(false)
                .modelPromotionAllowed(false)
                .piiViolations(0L)
                .syntheticFixturesQuarantined(29L)
                .activeModel("2.2.0-hybrid-semantic-384d")
                .fallbackModel("1.0.0-deterministic")
                .build();

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot.builder()
                        .legitimateOutcomeSessions(0L)
                        .readinessReport(report)
                        .build();

        when(readinessMonitor.evaluateLiveTelemetry()).thenReturn(snapshot);

        Phase34TrainingHandoff handoff = readinessValidator.buildHandoff();

        assertNotNull(handoff);
        assertEquals("TRAINING_NOT_READY", handoff.getStatus());
        assertEquals(0L, handoff.getLegitimateOutcomeSessions());
        assertEquals(100L, handoff.getRequiredOutcomeSessions());
        assertFalse(handoff.isTrainingReady());
        assertFalse(handoff.isModelTrainingAllowed());
        assertFalse(handoff.isModelPromotionAllowed());
        assertEquals("2.2.0-hybrid-semantic-384d", handoff.getActiveModel());
        assertEquals("1.0.0-deterministic", handoff.getFallbackModel());
    }

    @Test
    @DisplayName("100+ outcomes -> TRAINING_READY, trainingReady=true, modelTrainingAllowed=true, but modelPromotionAllowed remains FALSE")
    void testAtOrAboveThreshold_ReadyButPromotionBlocked() {
        Phase31ReadinessReport report = Phase31ReadinessReport.builder()
                .status("TRAINING_READY")
                .legitimateOutcomeSessions(100L)
                .requiredThreshold(100L)
                .remainingOutcomeSessions(0L)
                .trainingReady(true)
                .modelTrainingAllowed(true)
                .modelPromotionAllowed(false)
                .piiViolations(0L)
                .syntheticFixturesQuarantined(29L)
                .activeModel("2.2.0-hybrid-semantic-384d")
                .fallbackModel("1.0.0-deterministic")
                .build();

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot.builder()
                        .legitimateOutcomeSessions(100L)
                        .readinessReport(report)
                        .build();

        when(readinessMonitor.evaluateLiveTelemetry()).thenReturn(snapshot);

        Phase34TrainingHandoff handoff = readinessValidator.buildHandoff();

        assertNotNull(handoff);
        assertEquals("TRAINING_READY", handoff.getStatus());
        assertEquals(100L, handoff.getLegitimateOutcomeSessions());
        assertTrue(handoff.isTrainingReady());
        assertTrue(handoff.isModelTrainingAllowed());
        assertFalse(handoff.isModelPromotionAllowed(), "Promotion MUST remain blocked");
    }

    @Test
    @DisplayName("PII or target leakage forces TRAINING_NOT_READY even if outcome count >= 100")
    void testGateValidation_FailsOnPIIOrLeakage() {
        assertFalse(readinessValidator.validateGate(99, 0, 0));
        assertTrue(readinessValidator.validateGate(100, 0, 0));
        assertFalse(readinessValidator.validateGate(105, 1, 0), "Fails on PII violation");
        assertFalse(readinessValidator.validateGate(105, 0, 1), "Fails on Target leakage violation");
    }
}
