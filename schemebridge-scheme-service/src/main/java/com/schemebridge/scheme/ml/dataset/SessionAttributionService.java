package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.ml.feature.ComparisonFeatures;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.SchemeFeatures;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatures;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import com.schemebridge.scheme.service.EligibilityEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 27: Offline Interaction-to-Outcome Attribution, Multi-level Relevance Labeling
 * and LTR Dataset Pipeline Service.
 *
 * Core Invariants:
 * 1. Statutory eligibility is the non-negotiable first gate.
 * 2. Relevance attribution is deterministic and terminal-state based.
 * 3. Synthetic test fixtures (e.g. historical 29 application_events) are quarantined and excluded.
 * 4. Dataset splits are strictly isolated at the sessionId level (0.00% leakage).
 * 5. Readiness status evaluates to TRAINING_NOT_READY when legitimate outcome sessions < 100.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAttributionService {

    public static final int OUTCOME_SESSION_THRESHOLD = 100;

    private static final Set<String> SYNTHETIC_FIXTURE_PATTERNS = Set.of(
            "citizen1",
            "citizen_sharma_65",
            "test_citizen",
            "citizen_user",
            "test_user",
            "fixture_",
            "synthetic",
            "demo_citizen"
    );

    private final EligibilityEngine eligibilityEngine;
    private final UserFeatureExtractor userFeatureExtractor;
    private final SchemeFeatureExtractor schemeFeatureExtractor;
    private final UserSchemeComparisonService userSchemeComparisonService;
    private final FeatureVectorBuilder featureVectorBuilder;

    /**
     * Determines whether an interaction event or session belongs to synthetic test fixtures.
     */
    public boolean isSyntheticFixture(String userId, String sessionId, String eventId) {
        if (userId != null) {
            String lower = userId.trim().toLowerCase();
            for (String pattern : SYNTHETIC_FIXTURE_PATTERNS) {
                if (lower.contains(pattern)) return true;
            }
        }
        if (sessionId != null) {
            String lower = sessionId.trim().toLowerCase();
            for (String pattern : SYNTHETIC_FIXTURE_PATTERNS) {
                if (lower.contains(pattern)) return true;
            }
        }
        if (eventId != null) {
            String lower = eventId.trim().toLowerCase();
            if (lower.startsWith("test_") || lower.startsWith("fixture_") || lower.contains("mock")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves terminal relevance grade monotonically from observed events.
     * Starts at IMPRESSED_UNENGAGED (Grade 0).
     */
    public RelevanceGrade resolveTerminalGrade(List<SessionInteractionEvent> events) {
        if (events == null || events.isEmpty()) {
            return RelevanceGrade.IMPRESSED_UNENGAGED;
        }
        RelevanceGrade terminal = RelevanceGrade.IMPRESSED_UNENGAGED;
        for (SessionInteractionEvent event : events) {
            if (event.isSyntheticFixture()) {
                continue; // Skip synthetic fixtures
            }
            RelevanceGrade grade = RelevanceGrade.fromEventType(event.getEventType());
            terminal = RelevanceGrade.resolveTerminalGrade(terminal, grade);
        }
        return terminal;
    }

    /**
     * Deterministically assigns session to TRAIN (70%), VALIDATION (15%), or TEST (15%)
     * using SHA-256 hash modulo 100.
     * Invariant: Never split by schemeCode alone; session-level isolation guarantees zero leakage.
     */
    public String assignSessionSplit(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "TRAIN";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sessionId.getBytes(StandardCharsets.UTF_8));
            int bucket = Math.abs(((hash[0] & 0xFF) << 8) | (hash[1] & 0xFF)) % 100;
            if (bucket < 70) {
                return "TRAIN";
            } else if (bucket < 85) {
                return "VALIDATION";
            } else {
                return "TEST";
            }
        } catch (NoSuchAlgorithmException e) {
            int bucket = Math.abs(sessionId.hashCode()) % 100;
            return bucket < 70 ? "TRAIN" : (bucket < 85 ? "VALIDATION" : "TEST");
        }
    }

    /**
     * Complete attribution and training pair generation pipeline.
     * Enforces EligibilityEngine as first gate.
     * Returns empty if statutory eligibility fails.
     */
    public Optional<TrainingPair> buildTrainingPair(
            CitizenEligibilityProfile profile,
            Scheme scheme,
            String sessionId,
            List<SessionInteractionEvent> events,
            Integer rank,
            Double score,
            String modelVersion
    ) {
        // Absolute first gate: Statutory Eligibility
        EligibilityEvaluationResult eval = eligibilityEngine.evaluate(profile, scheme);
        if (eval == null || eval.getStatus() != EligibilityStatus.ELIGIBLE) {
            log.debug("Scheme {} failed statutory eligibility for session {}. Bypassing feature extraction and pair generation.",
                    scheme != null ? scheme.getSchemeCode() : "null", sessionId);
            return Optional.empty();
        }

        // Feature extraction and comparison
        UserFeatures userFeatures = userFeatureExtractor.extractFromEligibilityProfile(profile);
        SchemeFeatures schemeFeatures = schemeFeatureExtractor.extractFromScheme(scheme, null);
        ComparisonFeatures comparisonFeatures = userSchemeComparisonService.compare(userFeatures, schemeFeatures);

        UserSchemeFeatureVector featureVector = featureVectorBuilder.buildVector(
                userFeatures,
                schemeFeatures,
                comparisonFeatures,
                rank,
                score,
                modelVersion
        );

        RelevanceGrade terminalGrade = resolveTerminalGrade(events);
        String split = assignSessionSplit(sessionId);

        TrainingPair pair = TrainingPair.builder()
                .pairId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .schemeCode(scheme.getSchemeCode())
                .featureVector(featureVector)
                .relevanceGrade(terminalGrade)
                .isStatutoryEligible(true)
                .split(split)
                .timestamp(System.currentTimeMillis())
                .build();

        return Optional.of(pair);
    }

    /**
     * Evaluates dataset readiness against legitimate outcome sessions threshold.
     */
    public TrainingReadinessStatus evaluateReadiness(int legitimateOutcomeSessions) {
        if (legitimateOutcomeSessions < OUTCOME_SESSION_THRESHOLD) {
            log.info("Training Readiness: TRAINING_NOT_READY (Legitimate outcome sessions: {} < required: {})",
                    legitimateOutcomeSessions, OUTCOME_SESSION_THRESHOLD);
            return TrainingReadinessStatus.TRAINING_NOT_READY;
        }
        return TrainingReadinessStatus.READY_FOR_TRAINING;
    }
}
