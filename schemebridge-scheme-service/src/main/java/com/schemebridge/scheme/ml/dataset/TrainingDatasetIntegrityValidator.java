package com.schemebridge.scheme.ml.dataset;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Phase 34: Training Dataset Integrity Validator.
 * Performs strict pre-training audits:
 * A. Outcome count threshold (>= 100)
 * B. Data integrity (0 malformed, 0 synthetic, 0 PII, 0 duplicate, 0 invalid sequence, 0 orphan)
 * C. Statutory eligibility authority (100% governed, 0.00% violation rate)
 * D. Target integrity (0 target leakage)
 * E. Split integrity (0 cross-split session leakage)
 * F. Class distribution diversity (reports INSUFFICIENT_CLASS_DIVERSITY if sample classes are missing)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingDatasetIntegrityValidator {

    public enum IntegrityStatus {
        PASS,
        FAIL,
        INSUFFICIENT_DATA,
        INSUFFICIENT_CLASS_DIVERSITY
    }

    @Getter
    @ToString
    @Builder
    public static class IntegrityValidationReport {
        private final IntegrityStatus status;
        private final boolean passed;
        private final long legitimateOutcomeCount;
        private final long malformedCount;
        private final long syntheticCount;
        private final long piiViolations;
        private final long duplicateCount;
        private final long orphanCount;
        private final long invalidSequenceCount;
        private final long crossSplitLeakage;
        private final long targetLeakageViolations;
        private final double statutoryEligibilityViolationRate;
        private final Map<String, Long> classDistribution;
        private final List<String> errorMessages;
    }

    /**
     * Validates a candidate dataset snapshot against pre-training integrity criteria.
     */
    public IntegrityValidationReport validate(
            long outcomeCount,
            long malformedCount,
            long syntheticCount,
            long piiViolations,
            long duplicateCount,
            long orphanCount,
            long invalidSequenceCount,
            long crossSplitLeakage,
            long targetLeakageViolations,
            double statutoryViolationRate,
            Map<String, Long> classDistribution
    ) {
        List<String> errors = new ArrayList<>();

        // A. Outcome count threshold
        if (outcomeCount < 100) {
            errors.add(String.format("Insufficient outcome sessions: %d < 100", outcomeCount));
            return buildReport(IntegrityStatus.INSUFFICIENT_DATA, false, outcomeCount,
                    malformedCount, syntheticCount, piiViolations, duplicateCount, orphanCount,
                    invalidSequenceCount, crossSplitLeakage, targetLeakageViolations,
                    statutoryViolationRate, classDistribution, errors);
        }

        // B. Data integrity
        if (malformedCount > 0) errors.add("Malformed records detected: " + malformedCount);
        if (syntheticCount > 0) errors.add("Unquarantined synthetic records detected: " + syntheticCount);
        if (piiViolations > 0) errors.add("PII violations detected: " + piiViolations);
        if (duplicateCount > 0) errors.add("Duplicate records detected: " + duplicateCount);
        if (orphanCount > 0) errors.add("Orphan records detected: " + orphanCount);
        if (invalidSequenceCount > 0) errors.add("Invalid sequences detected: " + invalidSequenceCount);

        // C. Statutory eligibility
        if (statutoryViolationRate > 0.0) {
            errors.add("Statutory eligibility violation rate exceeds 0.00%: " + statutoryViolationRate);
        }

        // D. Target integrity
        if (targetLeakageViolations > 0) {
            errors.add("Target leakage detected: " + targetLeakageViolations);
        }

        // E. Split integrity
        if (crossSplitLeakage > 0) {
            errors.add("Cross-split session leakage detected: " + crossSplitLeakage);
        }

        if (!errors.isEmpty()) {
            return buildReport(IntegrityStatus.FAIL, false, outcomeCount,
                    malformedCount, syntheticCount, piiViolations, duplicateCount, orphanCount,
                    invalidSequenceCount, crossSplitLeakage, targetLeakageViolations,
                    statutoryViolationRate, classDistribution, errors);
        }

        // F. Class distribution diversity
        // Must have representation across grades (at minimum positive conversions Grade 3 and non-conversions)
        if (classDistribution != null) {
            long converted = classDistribution.getOrDefault("GRADE_3_CONVERTED", 0L);
            long unengaged = classDistribution.getOrDefault("GRADE_0_IMPRESSED_UNENGAGED", 0L)
                    + classDistribution.getOrDefault("GRADE_1_VIEWED", 0L)
                    + classDistribution.getOrDefault("GRADE_2_INTENT_HIGH", 0L);

            if (converted == 0 || unengaged == 0) {
                errors.add("Insufficient class diversity: Need both positive (converted) and non-converted sessions for supervised training.");
                return buildReport(IntegrityStatus.INSUFFICIENT_CLASS_DIVERSITY, false, outcomeCount,
                        malformedCount, syntheticCount, piiViolations, duplicateCount, orphanCount,
                        invalidSequenceCount, crossSplitLeakage, targetLeakageViolations,
                        statutoryViolationRate, classDistribution, errors);
            }
        }

        return buildReport(IntegrityStatus.PASS, true, outcomeCount,
                malformedCount, syntheticCount, piiViolations, duplicateCount, orphanCount,
                invalidSequenceCount, crossSplitLeakage, targetLeakageViolations,
                statutoryViolationRate, classDistribution, errors);
    }

    private IntegrityValidationReport buildReport(
            IntegrityStatus status,
            boolean passed,
            long outcomeCount,
            long malformedCount,
            long syntheticCount,
            long piiViolations,
            long duplicateCount,
            long orphanCount,
            long invalidSequenceCount,
            long crossSplitLeakage,
            long targetLeakageViolations,
            double statutoryViolationRate,
            Map<String, Long> classDistribution,
            List<String> errors
    ) {
        return IntegrityValidationReport.builder()
                .status(status)
                .passed(passed)
                .legitimateOutcomeCount(outcomeCount)
                .malformedCount(malformedCount)
                .syntheticCount(syntheticCount)
                .piiViolations(piiViolations)
                .duplicateCount(duplicateCount)
                .orphanCount(orphanCount)
                .invalidSequenceCount(invalidSequenceCount)
                .crossSplitLeakage(crossSplitLeakage)
                .targetLeakageViolations(targetLeakageViolations)
                .statutoryEligibilityViolationRate(statutoryViolationRate)
                .classDistribution(classDistribution != null ? classDistribution : Map.of())
                .errorMessages(errors)
                .build();
    }
}
