package com.schemebridge.scheme.ml.dataset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Phase34DatasetIntegrityTest {

    private TrainingDatasetIntegrityValidator integrityValidator;

    @BeforeEach
    void setUp() {
        integrityValidator = new TrainingDatasetIntegrityValidator();
    }

    @Test
    @DisplayName("Outcome count < 100 fails integrity validation with INSUFFICIENT_DATA")
    void testBelow100Outcomes_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                99, 0, 0, 0, 0, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 79L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.INSUFFICIENT_DATA, report.getStatus());
        assertEquals(99, report.getLegitimateOutcomeCount());
    }

    @Test
    @DisplayName("Target leakage violation fails integrity validation")
    void testTargetLeakage_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 0, 0, 0, 0, 0, 1, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 80L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL, report.getStatus());
        assertEquals(1, report.getTargetLeakageViolations());
    }

    @Test
    @DisplayName("Synthetic data present fails integrity validation")
    void testSyntheticData_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 1, 0, 0, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 80L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL, report.getStatus());
        assertEquals(1, report.getSyntheticCount());
    }

    @Test
    @DisplayName("PII violation detected fails integrity validation")
    void testPIIViolation_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 1, 0, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 80L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL, report.getStatus());
        assertEquals(1, report.getPiiViolations());
    }

    @Test
    @DisplayName("Duplicate session count fails integrity validation")
    void testDuplicateSessions_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 0, 2, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 80L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL, report.getStatus());
        assertEquals(2, report.getDuplicateCount());
    }

    @Test
    @DisplayName("Cross-split session leakage fails integrity validation")
    void testSplitLeakage_Fails() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 0, 0, 0, 0, 1, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 20L, "GRADE_1_VIEWED", 80L)
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.FAIL, report.getStatus());
        assertEquals(1, report.getCrossSplitLeakage());
    }

    @Test
    @DisplayName("Single-class distribution reports INSUFFICIENT_CLASS_DIVERSITY")
    void testClassDiversity_Insufficient() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 0, 0, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_1_VIEWED", 100L) // Missing converted grade 3
        );

        assertFalse(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.INSUFFICIENT_CLASS_DIVERSITY, report.getStatus());
    }

    @Test
    @DisplayName("100 diverse, clean, eligible, non-synthetic records PASS integrity validation")
    void testValidDataset_Passes() {
        TrainingDatasetIntegrityValidator.IntegrityValidationReport report = integrityValidator.validate(
                100, 0, 0, 0, 0, 0, 0, 0, 0, 0.0,
                Map.of("GRADE_3_CONVERTED", 25L, "GRADE_1_VIEWED", 75L)
        );

        assertTrue(report.isPassed());
        assertEquals(TrainingDatasetIntegrityValidator.IntegrityStatus.PASS, report.getStatus());
        assertEquals(100, report.getLegitimateOutcomeCount());
        assertEquals(0, report.getPiiViolations());
        assertEquals(0, report.getTargetLeakageViolations());
        assertEquals(0, report.getSyntheticCount());
        assertEquals(0, report.getDuplicateCount());
        assertEquals(0, report.getCrossSplitLeakage());
    }
}
