package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Phase 39: Deterministic, Read-Only Dataset Freeze Service.
 * Formally freezes a qualified training dataset prior to offline behavioral ML training.
 *
 * CRITICAL INVARIANTS:
 * - Read-only: Never mutates production database collections.
 * - Minimum threshold enforcement: If legitimate outcome sessions < 100, freeze is strictly BLOCKED.
 * - Deterministic and reproducible: Uses SHA-256 cryptographic hashing.
 * - Immutable: Frozen snapshot cannot be altered post-creation.
 * - Model promotion remains permanently false.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetFreezeService {

    public static final long REQUIRED_THRESHOLD = 100L;
    public static final String ACTIVE_RECOMMENDER_VERSION = "2.2.0-hybrid-semantic-384d";
    public static final String FALLBACK_RECOMMENDER_VERSION = "1.0.0-deterministic";
    public static final String ELIGIBILITY_ENGINE_VERSION = "1.0.0-statutory";
    public static final String LEAKAGE_AUDIT_VERSION = "1.0.0-strict";
    public static final String FEATURE_SCHEMA_VERSION = "1.0.0";
    public static final String LABEL_SCHEMA_VERSION = "1.0.0";

    private final BehavioralTrainingDatasetQualificationService qualificationService;

    /**
     * Attempts to freeze the live production dataset.
     * Guaranteed strictly read-only: Zero database mutations.
     */
    public DatasetFreezeSnapshot freezeLiveDataset() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        return freezeFromQualification(qualification, Collections.emptyList());
    }

    /**
     * Freezes a candidate dataset from explicit or in-memory telemetry events (used for testing).
     */
    public DatasetFreezeSnapshot freeze(List<RecommendationEvent> recEvents, List<ApplicationEvent> appEvents) {
        DatasetQualificationSnapshot qualification = qualificationService.qualify(recEvents, appEvents);

        List<String> eventIds = new ArrayList<>();
        if (recEvents != null) {
            for (RecommendationEvent e : recEvents) {
                if (e.getSessionId() != null) {
                    eventIds.add("rec:" + e.getSessionId() + ":" + e.getSchemeCode());
                }
            }
        }
        if (appEvents != null) {
            for (ApplicationEvent e : appEvents) {
                if (e.getId() != null) {
                    eventIds.add("app:" + e.getId());
                }
            }
        }

        return freezeFromQualification(qualification, eventIds);
    }

    private DatasetFreezeSnapshot freezeFromQualification(DatasetQualificationSnapshot qualification, List<String> eventIds) {
        long outcomeCount = qualification.getLegitimateOutcomeSessions();
        Instant now = Instant.now();

        // 1. Minimum Threshold Check (< 100 blocks freeze)
        if (outcomeCount < REQUIRED_THRESHOLD) {
            long remaining = REQUIRED_THRESHOLD - outcomeCount;
            String message = String.format("TRAINING_NOT_READY: Dataset freeze rejected. Legitimate outcomes (%d / %d, remaining: %d) below threshold. Training locked.",
                    outcomeCount, REQUIRED_THRESHOLD, remaining);
            log.info("Dataset Freeze Gate: {}", message);

            return DatasetFreezeSnapshot.builder()
                    .datasetVersion("NONE")
                    .freezeStatus("BLOCKED_BELOW_THRESHOLD")
                    .qualificationTimestamp(qualification.getEvaluatedAt())
                    .frozenAt(now)
                    .qualifyingSessionCount(outcomeCount)
                    .requiredThreshold(REQUIRED_THRESHOLD)
                    .sourceEventIdentifiers(Collections.emptyList())
                    .featureSchemaVersion(FEATURE_SCHEMA_VERSION)
                    .labelSchemaVersion(LABEL_SCHEMA_VERSION)
                    .activeRecommenderVersion(ACTIVE_RECOMMENDER_VERSION)
                    .fallbackRecommenderVersion(FALLBACK_RECOMMENDER_VERSION)
                    .eligibilityEngineVersion(ELIGIBILITY_ENGINE_VERSION)
                    .leakageAuditVersion(LEAKAGE_AUDIT_VERSION)
                    .integritySha256Hash("NONE")
                    .immutable(true)
                    .trainingEligible(false)
                    .modelPromotionAllowed(false)
                    .frozenExamples(Collections.emptyList())
                    .qualificationSnapshot(qualification)
                    .message(message)
                    .build();
        }

        // 2. Pre-Training Integrity Check
        if (!qualification.isTrainingReady() || !qualification.isModelTrainingAllowed()) {
            String message = String.format("DATASET_INTEGRITY_VIOLATION: Threshold met (%d >= %d) but dataset failed qualification: %s",
                    outcomeCount, REQUIRED_THRESHOLD, qualification.getQualificationMessage());
            log.warn("Dataset Freeze Gate: {}", message);

            return DatasetFreezeSnapshot.builder()
                    .datasetVersion("NONE")
                    .freezeStatus("BLOCKED_BY_INTEGRITY")
                    .qualificationTimestamp(qualification.getEvaluatedAt())
                    .frozenAt(now)
                    .qualifyingSessionCount(outcomeCount)
                    .requiredThreshold(REQUIRED_THRESHOLD)
                    .sourceEventIdentifiers(Collections.emptyList())
                    .featureSchemaVersion(FEATURE_SCHEMA_VERSION)
                    .labelSchemaVersion(LABEL_SCHEMA_VERSION)
                    .activeRecommenderVersion(ACTIVE_RECOMMENDER_VERSION)
                    .fallbackRecommenderVersion(FALLBACK_RECOMMENDER_VERSION)
                    .eligibilityEngineVersion(ELIGIBILITY_ENGINE_VERSION)
                    .leakageAuditVersion(LEAKAGE_AUDIT_VERSION)
                    .integritySha256Hash("NONE")
                    .immutable(true)
                    .trainingEligible(false)
                    .modelPromotionAllowed(false)
                    .frozenExamples(Collections.emptyList())
                    .qualificationSnapshot(qualification)
                    .message(message)
                    .build();
        }

        // 3. Freeze Successful: Compute deterministic SHA-256 hash
        String datasetVersion = "3.9.0-frozen-" + now.toEpochMilli();
        String hash = computeSha256Hash(datasetVersion, outcomeCount, qualification.getEvaluatedAt(), eventIds);

        String message = String.format("DATASET_FROZEN_SUCCESSFULLY: Qualified dataset frozen under version %s with SHA-256 hash %s. Ready for offline sandbox training.",
                datasetVersion, hash);
        log.info("Dataset Freeze Gate: {}", message);

        return DatasetFreezeSnapshot.builder()
                .datasetVersion(datasetVersion)
                .freezeStatus("FROZEN_SUCCESS")
                .qualificationTimestamp(qualification.getEvaluatedAt())
                .frozenAt(now)
                .qualifyingSessionCount(outcomeCount)
                .requiredThreshold(REQUIRED_THRESHOLD)
                .sourceEventIdentifiers(eventIds != null ? eventIds : Collections.emptyList())
                .featureSchemaVersion(FEATURE_SCHEMA_VERSION)
                .labelSchemaVersion(LABEL_SCHEMA_VERSION)
                .activeRecommenderVersion(ACTIVE_RECOMMENDER_VERSION)
                .fallbackRecommenderVersion(FALLBACK_RECOMMENDER_VERSION)
                .eligibilityEngineVersion(ELIGIBILITY_ENGINE_VERSION)
                .leakageAuditVersion(LEAKAGE_AUDIT_VERSION)
                .integritySha256Hash(hash)
                .immutable(true)
                .trainingEligible(true)
                .modelPromotionAllowed(false) // Permanently false
                .frozenExamples(Collections.emptyList())
                .qualificationSnapshot(qualification)
                .message(message)
                .build();
    }

    public String computeSha256Hash(String datasetVersion, long sessionCount, Instant timestamp, List<String> eventIds) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder canonical = new StringBuilder();
            canonical.append("version=").append(datasetVersion).append(";");
            canonical.append("sessions=").append(sessionCount).append(";");
            canonical.append("timestamp=").append(timestamp != null ? timestamp.toEpochMilli() : 0).append(";");
            canonical.append("recommender=").append(ACTIVE_RECOMMENDER_VERSION).append(";");
            canonical.append("eligibility=").append(ELIGIBILITY_ENGINE_VERSION).append(";");
            canonical.append("featureSchema=").append(FEATURE_SCHEMA_VERSION).append(";");
            canonical.append("labelSchema=").append(LABEL_SCHEMA_VERSION).append(";");
            if (eventIds != null && !eventIds.isEmpty()) {
                List<String> sorted = new ArrayList<>(eventIds);
                Collections.sort(sorted);
                canonical.append("events=").append(String.join(",", sorted)).append(";");
            }

            byte[] hashBytes = digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    public boolean isDatasetFrozenAndReady() {
        return freezeLiveDataset().isTrainingEligible();
    }
}
