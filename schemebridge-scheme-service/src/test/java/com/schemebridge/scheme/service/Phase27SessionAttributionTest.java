package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.ml.dataset.AttributedSession;
import com.schemebridge.scheme.ml.dataset.RelevanceGrade;
import com.schemebridge.scheme.ml.dataset.SessionAttributionService;
import com.schemebridge.scheme.ml.dataset.SessionInteractionEvent;
import com.schemebridge.scheme.ml.dataset.TrainingPair;
import com.schemebridge.scheme.ml.dataset.TrainingReadinessStatus;
import com.schemebridge.scheme.ml.feature.ComparisonFeatures;
import com.schemebridge.scheme.ml.feature.FeatureMatchStatus;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase27SessionAttributionTest {

    @Mock
    private EligibilityEngine eligibilityEngine;

    private UserFeatureExtractor userFeatureExtractor;
    private SchemeFeatureExtractor schemeFeatureExtractor;
    private UserSchemeComparisonService userSchemeComparisonService;
    private FeatureVectorBuilder featureVectorBuilder;
    private SessionAttributionService sessionAttributionService;

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
    }

    private CitizenEligibilityProfile createEligibleCitizen() {
        CitizenEligibilityProfile profile = new CitizenEligibilityProfile();
        profile.setAge(28);
        profile.setState("MAHARASHTRA");
        profile.setOccupation("FARMER");
        profile.setAnnualIncome(150000.0);
        profile.setSocialCategory("OBC");
        profile.setGender("FEMALE");
        profile.setDisabilityStatus(false);
        return profile;
    }

    private Scheme createTestScheme(String schemeCode) {
        Scheme scheme = new Scheme();
        scheme.setSchemeCode(schemeCode);
        scheme.setSchemeLevel(SchemeLevel.CENTRAL);
        scheme.setStateOrUt("ALL");
        return scheme;
    }

    @Nested
    @DisplayName("End-to-End Pipeline Tests")
    class EndToEndPipelineTests {

        @Test
        @DisplayName("Complete Chain: Eligibility -> Feature Vector -> Event Attribution -> Relevance Grade -> Training Pair -> Readiness Gate -> Split")
        void testCompletePipelineChain() {
            CitizenEligibilityProfile profile = createEligibleCitizen();
            Scheme scheme = createTestScheme("SCH_P27_001");
            String sessionId = "sess_genuine_98421";

            // 1. Eligibility Engine confirms ELIGIBLE
            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode("SCH_P27_001")
                            .status(EligibilityStatus.ELIGIBLE)
                            .confidenceScore(1.0)
                            .build()
            );

            // 2. Interaction telemetry observed
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder()
                            .sessionId(sessionId)
                            .schemeCode("SCH_P27_001")
                            .eventType("RECOMMENDATION_SHOWN")
                            .timestamp(1000L)
                            .isSyntheticFixture(false)
                            .build(),
                    SessionInteractionEvent.builder()
                            .sessionId(sessionId)
                            .schemeCode("SCH_P27_001")
                            .eventType("SCHEME_VIEWED")
                            .timestamp(1050L)
                            .isSyntheticFixture(false)
                            .build(),
                    SessionInteractionEvent.builder()
                            .sessionId(sessionId)
                            .schemeCode("SCH_P27_001")
                            .eventType("APPLICATION_STARTED")
                            .timestamp(1100L)
                            .isSyntheticFixture(false)
                            .build()
            );

            // 3. Build training pair
            Optional<TrainingPair> pairOpt = sessionAttributionService.buildTrainingPair(
                    profile, scheme, sessionId, events, 1, 0.95, "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pairOpt).isPresent();
            TrainingPair pair = pairOpt.get();

            // 4. Verify pair properties
            assertThat(pair.getSessionId()).isEqualTo(sessionId);
            assertThat(pair.getSchemeCode()).isEqualTo("SCH_P27_001");
            assertThat(pair.isStatutoryEligible()).isTrue();
            assertThat(pair.getRelevanceGrade()).isEqualTo(RelevanceGrade.INTENT_HIGH);
            assertThat(pair.getRelevanceScore()).isEqualTo(2);
            assertThat(pair.getSplit()).isIn("TRAIN", "VALIDATION", "TEST");

            // 5. Feature vector validation
            assertThat(pair.getFeatureVector()).isNotNull();
            assertThat(pair.getFeatureVector().getUserFeatures().getAgeBucket()).isEqualTo("AGE_26_35");
            assertThat(pair.getFeatureVector().getUserFeatures().getOccupationCode()).isEqualTo("FARMER");
            assertThat(pair.getFeatureVector().getRankingContext().get("modelVersion")).isEqualTo("2.2.0-hybrid-semantic-384d");

            // 6. Dataset readiness gate with 0 outcomes
            TrainingReadinessStatus readiness = sessionAttributionService.evaluateReadiness(0);
            assertThat(readiness).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
        }
    }

    @Nested
    @DisplayName("Mandatory Statutory Eligibility Gate Tests")
    class EligibilityGateTests {

        @Test
        @DisplayName("Eligibility Gate: Ineligible scheme yields Optional.empty() and NO training pair")
        void testIneligibleSchemeRejected() {
            CitizenEligibilityProfile profile = createEligibleCitizen();
            Scheme scheme = createTestScheme("SCH_P27_INELIGIBLE");
            String sessionId = "sess_ineligible_123";

            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode("SCH_P27_INELIGIBLE")
                            .status(EligibilityStatus.NOT_ELIGIBLE)
                            .confidenceScore(0.0)
                            .build()
            );

            Optional<TrainingPair> pairOpt = sessionAttributionService.buildTrainingPair(
                    profile, scheme, sessionId, List.of(), 1, 0.9, "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pairOpt).isEmpty();
        }

        @Test
        @DisplayName("Eligibility Gate: Insufficient data yields Optional.empty() and NO training pair")
        void testInsufficientDataSchemeRejected() {
            CitizenEligibilityProfile profile = createEligibleCitizen();
            Scheme scheme = createTestScheme("SCH_P27_INSUFFICIENT");
            String sessionId = "sess_insufficient_123";

            when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode("SCH_P27_INSUFFICIENT")
                            .status(EligibilityStatus.INSUFFICIENT_DATA)
                            .confidenceScore(0.0)
                            .build()
            );

            Optional<TrainingPair> pairOpt = sessionAttributionService.buildTrainingPair(
                    profile, scheme, sessionId, List.of(), 1, 0.9, "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pairOpt).isEmpty();
        }

        @Test
        @DisplayName("TrainingPair Invariant: Constructor throws if isStatutoryEligible is false")
        void testTrainingPairInvariantThrowsOnIneligible() {
            assertThatThrownBy(() -> TrainingPair.builder()
                    .pairId("pair_123")
                    .sessionId("sess_123")
                    .schemeCode("SCH_TEST")
                    .featureVector(featureVectorBuilder.buildVector(null, null, null, 1, 0.9, "test"))
                    .relevanceGrade(RelevanceGrade.CONVERTED)
                    .isStatutoryEligible(false)
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Statutory Eligibility Invariant Violated");
        }
    }

    @Nested
    @DisplayName("Monotonic Terminal-State Relevance Attribution")
    class TerminalStateAttributionTests {

        @Test
        @DisplayName("Attribution: Empty events produce Grade 0 (IMPRESSED_UNENGAGED)")
        void testEmptyEventsProduceGrade0() {
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(List.of());
            assertThat(grade).isEqualTo(RelevanceGrade.IMPRESSED_UNENGAGED);
            assertThat(grade.getGrade()).isEqualTo(0);
        }

        @Test
        @DisplayName("Attribution: RECOMMENDATION_SHOWN produces Grade 0")
        void testShownProducesGrade0() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            assertThat(grade).isEqualTo(RelevanceGrade.IMPRESSED_UNENGAGED);
            assertThat(grade.getGrade()).isEqualTo(0);
        }

        @Test
        @DisplayName("Attribution: SCHEME_VIEWED / EXPANDED produces Grade 1")
        void testViewedProducesGrade1() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_EXPANDED").build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            assertThat(grade).isEqualTo(RelevanceGrade.VIEWED);
            assertThat(grade.getGrade()).isEqualTo(1);
        }

        @Test
        @DisplayName("Attribution: SCHEME_SAVED / APPLICATION_STARTED produces Grade 2")
        void testStartedProducesGrade2() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_STARTED").build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            assertThat(grade).isEqualTo(RelevanceGrade.INTENT_HIGH);
            assertThat(grade.getGrade()).isEqualTo(2);
        }

        @Test
        @DisplayName("Attribution: APPLICATION_COMPLETED / SCHEME_APPLIED produces Grade 3")
        void testCompletedProducesGrade3() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_STARTED").build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_COMPLETED").build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            assertThat(grade).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(grade.getGrade()).isEqualTo(3);
        }

        @Test
        @DisplayName("Attribution: Monotonic terminal state never regresses on subsequent lower events")
        void testMonotonicityNeverRegresses() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder().eventType("APPLICATION_COMPLETED").build(),
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            assertThat(grade).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(grade.getGrade()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Synthetic Fixture Quarantine Tests")
    class SyntheticFixtureTests {

        @Test
        @DisplayName("Synthetic Fixtures: Identified and quarantined from training data")
        void testSyntheticFixtureDetection() {
            assertThat(sessionAttributionService.isSyntheticFixture("citizen1", "sess_001", "evt_001")).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("citizen_sharma_65", "sess_002", "evt_002")).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("test_citizen_42", "sess_003", "evt_003")).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("citizen_real_892", "fixture_session", "evt_004")).isTrue();
            assertThat(sessionAttributionService.isSyntheticFixture("citizen_real_892", "sess_prod", "test_evt")).isTrue();

            // Genuine citizen
            assertThat(sessionAttributionService.isSyntheticFixture("usr_a89bc23d", "sess_7f394e1", "evt_019a84")).isFalse();
        }

        @Test
        @DisplayName("Synthetic Fixtures: Events marked as synthetic are completely excluded from terminal attribution")
        void testSyntheticEventsExcludedFromAttribution() {
            List<SessionInteractionEvent> events = List.of(
                    SessionInteractionEvent.builder()
                            .eventType("APPLICATION_COMPLETED")
                            .isSyntheticFixture(true) // synthetic fixture event!
                            .build(),
                    SessionInteractionEvent.builder()
                            .eventType("SCHEME_VIEWED")
                            .isSyntheticFixture(false) // genuine event
                            .build()
            );
            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(events);
            // Grade 3 (APPLICATION_COMPLETED) must be ignored because it is synthetic fixture
            assertThat(grade).isEqualTo(RelevanceGrade.VIEWED);
            assertThat(grade.getGrade()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Zero-PII Telemetry Protection Tests")
    class ZeroPiiProtectionTests {

        @Test
        @DisplayName("Zero PII: Strips forbidden PII attributes from telemetry metadata")
        void testPiiSanitization() {
            Map<String, Object> dirtyMetadata = new HashMap<>();
            dirtyMetadata.put("aadhaar_number", "1234-5678-9012");
            dirtyMetadata.put("phone_number", "+919876543210");
            dirtyMetadata.put("user_name", "Rajesh Kumar");
            dirtyMetadata.put("email", "citizen@example.com");
            dirtyMetadata.put("ip_address", "192.168.1.1");
            dirtyMetadata.put("ui_theme", "DARK");
            dirtyMetadata.put("viewport_width", 1280);

            Map<String, Object> cleanMetadata = SessionInteractionEvent.sanitizeMetadata(dirtyMetadata);

            assertThat(cleanMetadata).doesNotContainKeys(
                    "aadhaar_number", "phone_number", "user_name", "email", "ip_address"
            );
            assertThat(cleanMetadata).containsEntry("ui_theme", "DARK");
            assertThat(cleanMetadata).containsEntry("viewport_width", 1280);
        }
    }

    @Nested
    @DisplayName("Session-Level Leakage Prevention Tests")
    class SessionLeakageTests {

        @Test
        @DisplayName("Leakage: Strict session-level isolation, zero overlap across TRAIN/VAL/TEST")
        void testSessionLevelIsolation() {
            Set<String> trainSessions = new HashSet<>();
            Set<String> valSessions = new HashSet<>();
            Set<String> testSessions = new HashSet<>();

            for (int i = 0; i < 200; i++) {
                String sessionId = "session_uuid_" + i;
                String split = sessionAttributionService.assignSessionSplit(sessionId);

                // Determinism test: calling repeatedly produces exact same split
                String split2 = sessionAttributionService.assignSessionSplit(sessionId);
                assertThat(split).isEqualTo(split2);

                if ("TRAIN".equals(split)) trainSessions.add(sessionId);
                else if ("VALIDATION".equals(split)) valSessions.add(sessionId);
                else if ("TEST".equals(split)) testSessions.add(sessionId);
            }

            // Zero leakage check
            Set<String> trainValIntersect = new HashSet<>(trainSessions);
            trainValIntersect.retainAll(valSessions);
            assertThat(trainValIntersect).isEmpty();

            Set<String> trainTestIntersect = new HashSet<>(trainSessions);
            trainTestIntersect.retainAll(testSessions);
            assertThat(trainTestIntersect).isEmpty();

            Set<String> valTestIntersect = new HashSet<>(valSessions);
            valTestIntersect.retainAll(testSessions);
            assertThat(valTestIntersect).isEmpty();

            // Sanity check all sessions categorized
            assertThat(trainSessions.size() + valSessions.size() + testSessions.size()).isEqualTo(200);
            // TRAIN should be roughly 70%
            assertThat(trainSessions.size()).isGreaterThan(100);
        }
    }

    @Nested
    @DisplayName("Dataset Readiness Gate Tests")
    class DatasetReadinessGateTests {

        @Test
        @DisplayName("Readiness Gate: 0 legitimate outcome sessions strictly yields TRAINING_NOT_READY")
        void testZeroOutcomesYieldsTrainingNotReady() {
            TrainingReadinessStatus status = sessionAttributionService.evaluateReadiness(0);
            assertThat(status).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
        }

        @Test
        @DisplayName("Readiness Gate: 50 legitimate outcome sessions (<100) strictly yields TRAINING_NOT_READY")
        void testSubThresholdYieldsTrainingNotReady() {
            TrainingReadinessStatus status = sessionAttributionService.evaluateReadiness(50);
            assertThat(status).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
        }

        @Test
        @DisplayName("Readiness Gate: >=100 legitimate outcome sessions yields READY_FOR_TRAINING")
        void testAboveThresholdYieldsReady() {
            TrainingReadinessStatus status = sessionAttributionService.evaluateReadiness(120);
            assertThat(status).isEqualTo(TrainingReadinessStatus.READY_FOR_TRAINING);
        }
    }
}
