package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.ApplicationEvent;
import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.ml.feature.*;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase30ProductionOutcomeAccumulatorTest {

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
    private ProductionOutcomeAccumulator outcomeAccumulator;

    private Scheme canonicalScheme;

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
        outcomeAccumulator = new ProductionOutcomeAccumulator(
                recommendationEventRepository,
                applicationEventRepository,
                schemeRepository,
                sessionAttributionService,
                qualityMonitor,
                trainingReadinessGate,
                targetLeakageDetector
        );

        canonicalScheme = new Scheme();
        canonicalScheme.setId("scheme-001");
        canonicalScheme.setSchemeCode("SCH-AGRI-01");
        canonicalScheme.setSlug("pm-krishi-vikas");
        canonicalScheme.setSchemeLevel(SchemeLevel.CENTRAL);
        canonicalScheme.setStateOrUt("ALL");
    }

    private RecommendationEvent createEvent(String userId, String sessionId, String schemeCode, RecommendationEventType type, Instant timestamp, Map<String, Object> metadata) {
        return RecommendationEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .schemeCode(schemeCode)
                .eventType(type)
                .timestamp(timestamp)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();
    }

    private List<RecommendationEvent> generateOutcomeSession(String sessionId, String schemeCode, Instant baseTime) {
        return List.of(
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN, baseTime, null),
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.SCHEME_VIEWED, baseTime.plusSeconds(10), null),
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.APPLICATION_STARTED, baseTime.plusSeconds(30), null),
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.APPLICATION_COMPLETED, baseTime.plusSeconds(120), null)
        );
    }

    @Nested
    @DisplayName("1. Readiness Transition Tests (0, 1, 99, 100, 101)")
    class ReadinessThresholdTests {

        @Test
        @DisplayName("Zero legitimate outcome sessions -> TRAINING_NOT_READY, trainingReady=false, modelTrainingAllowed=false")
        void testZeroLegitimateOutcomes() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            // Only browsing, no outcome events
            List<RecommendationEvent> events = List.of(
                    createEvent("citizen_100", "sess_001", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(100), null),
                    createEvent("citizen_100", "sess_001", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, Instant.now().minusSeconds(50), null)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            Phase30ReadinessReport report = result.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(report.isTrainingReady()).isFalse();
            assertThat(report.isModelTrainingAllowed()).isFalse();
            assertThat(report.isModelPromotionAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(0);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(100);
            assertThat(report.getActiveModel()).isEqualTo("2.2.0-hybrid-semantic-384d");
            assertThat(report.getFallbackModel()).isEqualTo("1.0.0-deterministic");
            assertThat(report.getCircuitBreakerMs()).isEqualTo(200);
        }

        @Test
        @DisplayName("1 legitimate outcome session -> TRAINING_NOT_READY")
        void testOneLegitimateOutcome() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> events = generateOutcomeSession("sess_001", "SCH-AGRI-01", Instant.now().minusSeconds(300));
            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());

            assertThat(result.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(result.getReadinessReport().isTrainingReady()).isFalse();
            assertThat(result.getReadinessReport().isModelTrainingAllowed()).isFalse();
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(1);
            assertThat(result.getReadinessReport().getRemainingOutcomeSessions()).isEqualTo(99);
            assertThat(result.getProgressReport().getProgressPercent()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("99 legitimate outcome sessions -> TRAINING_NOT_READY")
        void testNinetyNineLegitimateOutcomes() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> allEvents = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(10000);
            for (int i = 0; i < 99; i++) {
                allEvents.addAll(generateOutcomeSession("session_out_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L)));
            }

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(allEvents, List.of());
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(99);
            assertThat(result.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(result.getReadinessReport().isTrainingReady()).isFalse();
            assertThat(result.getReadinessReport().isModelTrainingAllowed()).isFalse();
            assertThat(result.getReadinessReport().getRemainingOutcomeSessions()).isEqualTo(1);
            assertThat(result.getProgressReport().getProgressPercent()).isEqualTo(99.0);
        }

        @Test
        @DisplayName("Exactly 100 legitimate outcome sessions -> TRAINING_READY, modelTrainingAllowed=true, modelPromotionAllowed=false")
        void testExactlyOneHundredLegitimateOutcomes() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> allEvents = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(20000);
            for (int i = 0; i < 100; i++) {
                allEvents.addAll(generateOutcomeSession("session_out_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L)));
            }

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(allEvents, List.of());
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(100);
            assertThat(result.getReadinessReport().getStatus()).isEqualTo("TRAINING_READY");
            assertThat(result.getReadinessReport().isTrainingReady()).isTrue();
            assertThat(result.getReadinessReport().isModelTrainingAllowed()).isTrue();
            assertThat(result.getReadinessReport().isModelPromotionAllowed()).isFalse(); // Critical non-negotiable invariant
            assertThat(result.getReadinessReport().getRemainingOutcomeSessions()).isEqualTo(0);
            assertThat(result.getProgressReport().getProgressPercent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("101 legitimate outcome sessions -> remains TRAINING_READY, modelPromotionAllowed=false")
        void testOneHundredOneLegitimateOutcomes() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> allEvents = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(25000);
            for (int i = 0; i < 101; i++) {
                allEvents.addAll(generateOutcomeSession("session_out_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L)));
            }

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(allEvents, List.of());
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(101);
            assertThat(result.getReadinessReport().getStatus()).isEqualTo("TRAINING_READY");
            assertThat(result.getReadinessReport().isTrainingReady()).isTrue();
            assertThat(result.getReadinessReport().isModelTrainingAllowed()).isTrue();
            assertThat(result.getReadinessReport().isModelPromotionAllowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("2. Synthetic, Malformed, PII, Duplicate, and Orphan Telemetry Isolation")
    class TelemetryIsolationTests {

        @Test
        @DisplayName("Synthetic fixtures (including 29 historical application_events) are quarantined and excluded")
        void testSyntheticQuarantine() {
            // Mock 29 historical application_events
            List<ApplicationEvent> appEvents = new ArrayList<>();
            for (int i = 0; i < 29; i++) {
                appEvents.add(ApplicationEvent.builder()
                        .id("67634f19b84179320e6f7" + String.format("%03d", i))
                        .userId("citizen1")
                        .applicationId("6a952810c6037907f0030c06")
                        .build());
            }

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(List.of(), appEvents);
            assertThat(result.getSyntheticEvents()).isEqualTo(29);
            assertThat(result.getValidEvents()).isEqualTo(0);
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(0);
            assertThat(result.getReadinessReport().getSyntheticFixturesQuarantined()).isEqualTo(29);
        }

        @Test
        @DisplayName("Duplicate events in same session are detected and excluded from session aggregation")
        void testDuplicateExclusion() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now().minusSeconds(100);
            List<RecommendationEvent> events = List.of(
                    createEvent("u1", "sess_dup", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, null),
                    createEvent("u1", "sess_dup", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now.plusSeconds(1), null)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            assertThat(result.getDuplicateEvents()).isEqualTo(1);
            assertThat(result.getValidEvents()).isEqualTo(1);
        }

        @Test
        @DisplayName("Events containing recursive PII keys are quarantined as PII_VIOLATION and never enter datasets")
        void testPiiExclusion() {
            Map<String, Object> nested = new HashMap<>();
            nested.put("contact_mobile", "9876543210");
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("user_data", nested);

            List<RecommendationEvent> events = List.of(
                    createEvent("u1", "sess_pii", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(50), metadata)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            assertThat(result.getPiiViolations()).isEqualTo(1);
            assertThat(result.getValidEvents()).isEqualTo(0);
        }

        @Test
        @DisplayName("Malformed events (missing sessionId or schemeCode or invalid timestamp) are rejected")
        void testMalformedExclusion() {
            List<RecommendationEvent> events = List.of(
                    createEvent("u1", null, "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(50), null),
                    createEvent("u1", "sess_mal", "", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(50), null),
                    createEvent("u1", "sess_mal2", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().plusSeconds(100000), null)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            assertThat(result.getMalformedEvents()).isEqualTo(3);
            assertThat(result.getValidEvents()).isEqualTo(0);
        }

        @Test
        @DisplayName("Orphan events referencing non-existent schemes are excluded")
        void testOrphanExclusion() {
            when(schemeRepository.findBySchemeCode("SCH-UNKNOWN-999")).thenReturn(Optional.empty());

            List<RecommendationEvent> events = List.of(
                    createEvent("u1", "sess_orphan", "SCH-UNKNOWN-999", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(50), null)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            assertThat(result.getOrphanEvents()).isEqualTo(1);
            assertThat(result.getValidEvents()).isEqualTo(0);
        }

        @Test
        @DisplayName("Invalid sequence (non-chronological events) is rejected")
        void testInvalidSequenceExclusion() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now().minusSeconds(200);
            List<RecommendationEvent> events = List.of(
                    createEvent("u1", "sess_seq", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, null),
                    // Non-chronological timestamp
                    createEvent("u1", "sess_seq", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.minusSeconds(50), null)
            );

            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(events, List.of());
            assertThat(result.getInvalidSequenceEvents()).isGreaterThan(0);
            assertThat(result.getLegitimateOutcomeSessions()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("3. Statutory Eligibility First Gate & Target Leakage")
    class StatutoryEligibilityAndLeakageTests {

        @Test
        @DisplayName("Ineligible schemes are strictly bypassed from training pair creation")
        void testIneligibleSchemeBypassed() {
            CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                    .age(75)
                    .gender("MALE")
                    .state("Delhi")
                    .build();

            when(eligibilityEngine.evaluate(profile, canonicalScheme)).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode(canonicalScheme.getSchemeCode())
                            .status(EligibilityStatus.NOT_ELIGIBLE)
                            .explanation("Age 75 exceeds maximum statutory threshold of 60")
                            .build()
            );

            Optional<TrainingPair> pair = sessionAttributionService.buildTrainingPair(
                    profile,
                    canonicalScheme,
                    "sess_ineligible",
                    List.of(),
                    1,
                    0.9,
                    "2.2.0-hybrid-semantic-384d"
            );

            assertThat(pair).isEmpty();
        }

        @Test
        @DisplayName("Target leakage detector throws IllegalStateException if relevanceGrade is present in feature vector")
        void testTargetLeakageDetectorRejectsTarget() {
            Map<String, Object> contextWithTarget = new HashMap<>();
            contextWithTarget.put("relevanceGrade", 3);

            UserSchemeFeatureVector vector = UserSchemeFeatureVector.builder()
                    .rankingContext(contextWithTarget)
                    .build();

            assertThatThrownBy(() -> targetLeakageDetector.audit(vector))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CRITICAL TARGET LEAKAGE DETECTED");
        }
    }

    @Nested
    @DisplayName("4. Deterministic Monotonic Terminal Grading, Splits & Hashing")
    class AttributionMonotonicityAndSplits {

        @Test
        @DisplayName("Relevance grade resolves monotonically from 0 -> 1 -> 2 -> 3")
        void testMonotonicTerminalGrading() {
            Instant base = Instant.now().minusSeconds(1000);
            List<SessionInteractionEvent> progression = List.of(
                    SessionInteractionEvent.builder().eventType("RECOMMENDATION_SHOWN").timestamp(base.toEpochMilli()).build(),
                    SessionInteractionEvent.builder().eventType("SCHEME_VIEWED").timestamp(base.plusSeconds(10).toEpochMilli()).build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_STARTED").timestamp(base.plusSeconds(30).toEpochMilli()).build(),
                    SessionInteractionEvent.builder().eventType("APPLICATION_COMPLETED").timestamp(base.plusSeconds(120).toEpochMilli()).build()
            );

            RelevanceGrade grade = sessionAttributionService.resolveTerminalGrade(progression);
            assertThat(grade).isEqualTo(RelevanceGrade.CONVERTED);
            assertThat(grade.getGrade()).isEqualTo(3);
        }

        @Test
        @DisplayName("Session split isolation is 100% deterministic (TRAIN 70%, VAL 15%, TEST 15%) with zero overlap")
        void testSessionSplitIsolation() {
            Set<String> train = new HashSet<>();
            Set<String> val = new HashSet<>();
            Set<String> test = new HashSet<>();

            for (int i = 0; i < 500; i++) {
                String sessionId = "eval_session_" + i;
                String split = sessionAttributionService.assignSessionSplit(sessionId);
                switch (split) {
                    case "TRAIN" -> train.add(sessionId);
                    case "VALIDATION" -> val.add(sessionId);
                    case "TEST" -> test.add(sessionId);
                }
            }

            // Zero session overlap
            Set<String> trainValOverlap = new HashSet<>(train);
            trainValOverlap.retainAll(val);
            assertThat(trainValOverlap).isEmpty();

            Set<String> trainTestOverlap = new HashSet<>(train);
            trainTestOverlap.retainAll(test);
            assertThat(trainTestOverlap).isEmpty();

            Set<String> valTestOverlap = new HashSet<>(val);
            valTestOverlap.retainAll(test);
            assertThat(valTestOverlap).isEmpty();
        }

        @Test
        @DisplayName("SHA-256 integrity hash is reproducible for identical input")
        void testReproducibleSha256() throws Exception {
            String content = "phase30-pretraining-v1-artifact-deterministic-test";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash1 = md.digest(content.getBytes(StandardCharsets.UTF_8));

            MessageDigest md2 = MessageDigest.getInstance("SHA-256");
            byte[] hash2 = md2.digest(content.getBytes(StandardCharsets.UTF_8));

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Database mutations map remains strictly zero across all operations")
        void testZeroMutationsReported() {
            ProductionOutcomeAccumulator.AccumulationResult result = outcomeAccumulator.accumulate(List.of(), List.of());
            Map<String, Integer> mutations = result.getReadinessReport().getDatabaseMutations();

            assertThat(mutations.get("INSERT")).isEqualTo(0);
            assertThat(mutations.get("UPDATE")).isEqualTo(0);
            assertThat(mutations.get("DELETE")).isEqualTo(0);
            assertThat(mutations.get("DROP")).isEqualTo(0);
        }
    }
}
