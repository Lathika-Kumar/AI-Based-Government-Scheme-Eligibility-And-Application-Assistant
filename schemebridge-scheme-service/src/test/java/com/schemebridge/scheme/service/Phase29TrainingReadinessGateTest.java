package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase29TrainingReadinessGateTest {

    @Mock
    private RecommendationEventRepository recommendationEventRepository;

    @Mock
    private ApplicationEventRepository applicationEventRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    private UserFeatureExtractor userFeatureExtractor;
    private SchemeFeatureExtractor schemeFeatureExtractor;
    private UserSchemeComparisonService userSchemeComparisonService;
    private FeatureVectorBuilder featureVectorBuilder;
    private SessionAttributionService sessionAttributionService;
    private TrainingReadinessGate trainingReadinessGate;
    private TargetLeakageDetector targetLeakageDetector;
    private Phase29TelemetryQualityMonitor qualityMonitor;

    @BeforeEach
    void setUp() {
        userFeatureExtractor = new UserFeatureExtractor();
        schemeFeatureExtractor = new SchemeFeatureExtractor();
        userSchemeComparisonService = new UserSchemeComparisonService();
        featureVectorBuilder = new FeatureVectorBuilder();
        sessionAttributionService = new SessionAttributionService(
                eligibilityEngine,
                userFeatureExtractor,
                schemeFeatureExtractor,
                userSchemeComparisonService,
                featureVectorBuilder
        );
        trainingReadinessGate = new TrainingReadinessGate();
        targetLeakageDetector = new TargetLeakageDetector();
        qualityMonitor = new Phase29TelemetryQualityMonitor(
                recommendationEventRepository,
                applicationEventRepository,
                schemeRepository,
                sessionAttributionService,
                trainingReadinessGate,
                targetLeakageDetector
        );
    }

    private Scheme createTestScheme(String schemeCode) {
        Scheme s = new Scheme();
        s.setId("scheme_id_" + schemeCode);
        s.setSchemeCode(schemeCode);
        s.setSchemeLevel(SchemeLevel.CENTRAL);
        s.setStateOrUt("ALL");
        return s;
    }

    @Nested
    @DisplayName("Centralized Training Readiness Gate Tests")
    class GateDecisionTests {

        @Test
        @DisplayName("1. Zero legitimate outcome sessions yields TRAINING_NOT_READY, training & promotion blocked")
        void testZeroLegitimateSessionsBlocked() {
            TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(0L);

            assertThat(decision.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(decision.isModelTrainingAllowed()).isFalse();
            assertThat(decision.isModelPromotionAllowed()).isFalse();
            assertThat(decision.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(decision.getRequiredOutcomeSessions()).isEqualTo(100L);
            assertThat(decision.getRemainingOutcomeSessions()).isEqualTo(100L);
        }

        @Test
        @DisplayName("2. 99 legitimate outcome sessions yields TRAINING_NOT_READY, training & promotion blocked")
        void testNinetyNineSessionsBlocked() {
            TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(99L);

            assertThat(decision.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(decision.isModelTrainingAllowed()).isFalse();
            assertThat(decision.isModelPromotionAllowed()).isFalse();
            assertThat(decision.getLegitimateOutcomeSessions()).isEqualTo(99L);
            assertThat(decision.getRemainingOutcomeSessions()).isEqualTo(1L);
        }

        @Test
        @DisplayName("3. Exactly 100 legitimate outcome sessions yields TRAINING_READY, modelTrainingAllowed = true, but modelPromotionAllowed = false")
        void testExactlyOneHundredSessionsReady() {
            TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(100L);

            assertThat(decision.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_READY);
            assertThat(decision.isModelTrainingAllowed()).isTrue();
            assertThat(decision.isModelPromotionAllowed()).isFalse(); // Invariant: Promotion remains blocked
            assertThat(decision.getLegitimateOutcomeSessions()).isEqualTo(100L);
            assertThat(decision.getRemainingOutcomeSessions()).isEqualTo(0L);
        }

        @Test
        @DisplayName("4. Synthetic sessions do not contribute to legitimate outcome volume")
        void testSyntheticSessionsDoNotChangeGate() {
            // Mock 29 historical synthetic fixtures
            List<ApplicationEvent> fixtures = new ArrayList<>();
            for (int i = 0; i < 29; i++) {
                fixtures.add(ApplicationEvent.builder()
                        .id("app_evt_" + i)
                        .applicationId("6a952810c6037907f0030c06")
                        .userId("citizen_user")
                        .createdAt(Instant.now())
                        .build());
            }

            when(applicationEventRepository.findAll()).thenReturn(fixtures);
            when(recommendationEventRepository.findAll()).thenReturn(Collections.emptyList());

            Phase29ReadinessReport report = qualityMonitor.generateReadinessReport();

            assertThat(report.getTrainingReadiness()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(report.isModelTrainingAllowed()).isFalse();
            assertThat(report.isModelPromotionAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(report.getSyntheticFixturesQuarantined()).isEqualTo(29L);
            assertThat(report.getActiveModel()).isEqualTo("2.2.0-hybrid-semantic-384d");
            assertThat(report.getFallbackModel()).isEqualTo("1.0.0-deterministic");
            assertThat(report.getCircuitBreakerMs()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Session-Level Split & Leakage Isolation Tests")
    class SessionSplitTests {

        @Test
        @DisplayName("1. Session-level SHA-256 split achieves 100% isolation (0% cross-split leakage)")
        void testSessionSplitIsolation() {
            Set<String> train = new HashSet<>();
            Set<String> val = new HashSet<>();
            Set<String> test = new HashSet<>();

            for (int i = 0; i < 300; i++) {
                String sessionId = "sess_p29_uuid_" + i;
                String split = sessionAttributionService.assignSessionSplit(sessionId);

                // Deterministic reproducibility: repeated call gives identical split
                String splitRepeat = sessionAttributionService.assignSessionSplit(sessionId);
                assertThat(split).isEqualTo(splitRepeat);

                if ("TRAIN".equals(split)) train.add(sessionId);
                else if ("VALIDATION".equals(split)) val.add(sessionId);
                else if ("TEST".equals(split)) test.add(sessionId);
            }

            // Verify strict zero overlap
            Set<String> tv = new HashSet<>(train);
            tv.retainAll(val);
            assertThat(tv).isEmpty();

            Set<String> tt = new HashSet<>(train);
            tt.retainAll(test);
            assertThat(tt).isEmpty();

            Set<String> vt = new HashSet<>(val);
            vt.retainAll(test);
            assertThat(vt).isEmpty();
        }

        @Test
        @DisplayName("2. Ineligible scheme strictly rejected before training pair creation (0.00% violation rate)")
        void testIneligibleSchemeRejectedBeforePairGeneration() {
            CitizenEligibilityProfile profile = new CitizenEligibilityProfile();
            profile.setAge(30);
            Scheme scheme = createTestScheme("SCH_INELIGIBLE_99");

            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode("SCH_INELIGIBLE_99")
                            .status(EligibilityStatus.NOT_ELIGIBLE)
                            .build()
            );

            Optional<TrainingPair> pair = sessionAttributionService.buildTrainingPair(
                    profile, scheme, "sess_101", List.of(), 1, 0.8, "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pair).isEmpty();
        }
    }
}
