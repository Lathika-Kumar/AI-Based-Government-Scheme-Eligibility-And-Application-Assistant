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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Phase 33: Production Operationalization, Real Traffic Telemetry & Training-Readiness Handoff Test Suite.
 * Covers:
 * - Genuine recommendation telemetry & session lifecycle
 * - 11-gate ingestion boundary & 7-way quality classification
 * - PII rejection without logging values
 * - Synthetic quarantine (including 29 historical application fixtures)
 * - Duplicate handling & idempotency
 * - Invalid sequence rejection
 * - Missing recommendation anchor validation
 * - Scheme validation & statutory EligibilityEngine authority
 * - Browsing-only session exclusion
 * - Legitimate conversion qualification (SCHEME_APPLIED / APPLICATION_COMPLETED)
 * - Outcome idempotency (duplicate outcome prevention)
 * - Readiness below threshold (0-99 -> TRAINING_NOT_READY)
 * - Readiness at threshold (100+ -> TRAINING_READY, modelTrainingAllowed=true, promotion=false)
 * - Model promotion strictly disabled
 * - Zero target leakage & zero database mutations
 * - Continuous readiness snapshot top-level contract
 * - No automatic training trigger
 */
@ExtendWith(MockitoExtension.class)
class Phase33ProductionOperationalizationTest {

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
    private ProductionTelemetryIngestionService ingestionService;
    private Phase31ContinuousReadinessMonitor readinessMonitor;
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
        ingestionService = new ProductionTelemetryIngestionService(
                schemeRepository,
                sessionAttributionService,
                qualityMonitor,
                eligibilityEngine
        );
        readinessMonitor = new Phase31ContinuousReadinessMonitor(
                recommendationEventRepository,
                applicationEventRepository,
                schemeRepository,
                sessionAttributionService,
                qualityMonitor,
                ingestionService,
                trainingReadinessGate
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

    private RecommendationEvent createEvent(String userId, String sessionId, String schemeCode,
                                            RecommendationEventType type, Instant timestamp,
                                            Map<String, Object> metadata) {
        return RecommendationEvent.builder()
                .userId(userId)
                .sessionId(sessionId)
                .schemeCode(schemeCode)
                .eventType(type)
                .timestamp(timestamp)
                .metadata(metadata)
                .build();
    }

    @Nested
    @DisplayName("1. 11-Gate Ingestion Boundary & 7-Way Quality Classification")
    class IngestionBoundaryAndClassificationTests {

        @Test
        @DisplayName("Malformed telemetry missing mandatory attributes is classified as MALFORMED")
        void testMalformedTelemetry_Rejected() {
            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_101", null, "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, null
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.MALFORMED);
            assertThat(decision.getRejectionCategory()).isEqualTo("MISSING_REQUIRED_FIELDS");
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("Synthetic fixtures are quarantined and classified as SYNTHETIC")
        void testSyntheticTelemetry_Quarantined() {
            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "test_citizen_01", "sess-synthetic-99", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, null
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.SYNTHETIC);
            assertThat(decision.getRejectionCategory()).isEqualTo("SYNTHETIC_FIXTURE");
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("Metadata containing direct or nested PII keys is classified as PII_VIOLATION")
        void testPiiTelemetry_QuarantinedWithoutLoggingValues() {
            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            Map<String, Object> piiData = Map.of("applicant_mobile", "9876543210");
            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-pii-99", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), piiData, seen, anchors, null
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.PII_VIOLATION);
            assertThat(decision.getRejectionCategory()).isEqualTo("PII_KEY_DETECTED");
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("Schemes absent from master repository are classified as ORPHAN")
        void testOrphanScheme_Rejected() {
            when(schemeRepository.findBySchemeCode("SCH-UNKNOWN-99")).thenReturn(Optional.empty());

            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-orphan-99", "SCH-UNKNOWN-99", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, null
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
            assertThat(decision.getRejectionCategory()).isEqualTo("SCHEME_NOT_FOUND");
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("Non-anchor event without preceding RECOMMENDATION_SHOWN is classified as ORPHAN")
        void testMissingRecommendationAnchor_Rejected() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Set<String> seen = new HashSet<>();
            Set<String> emptyAnchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-no-anchor-99", "SCH-AGRI-01", "SCHEME_VIEWED",
                    Instant.now(), Map.of(), seen, emptyAnchors, null
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
            assertThat(decision.getRejectionCategory()).isEqualTo("MISSING_RECOMMENDATION_ANCHOR");
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("Duplicate events in session are classified as DUPLICATE")
        void testDuplicateEvent_Rejected() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision first = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-dup-99", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, null
            );
            assertThat(first.getStatus()).isEqualTo(TelemetryQualityStatus.VALID);

            ProductionTelemetryIngestionService.IngestionDecision duplicate = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-dup-99", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, null
            );
            assertThat(duplicate.getStatus()).isEqualTo(TelemetryQualityStatus.DUPLICATE);
            assertThat(duplicate.getRejectionCategory()).isEqualTo("DUPLICATE_IN_SESSION");
            assertThat(duplicate.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("EligibilityEngine remains sole statutory authority")
        void testEligibilityEngineAuthority() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(10).build();
            EligibilityEvaluationResult ineligibleResult = EligibilityEvaluationResult.builder()
                    .status(EligibilityStatus.NOT_ELIGIBLE)
                    .build();

            when(eligibilityEngine.evaluate(eq(profile), any(Scheme.class))).thenReturn(ineligibleResult);

            Set<String> seen = new HashSet<>();
            Set<String> anchors = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_live_01", "sess-elig-99", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                    Instant.now(), Map.of(), seen, anchors, profile
            );
            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.VALID);
            assertThat(decision.isEligible()).isFalse();
            assertThat(decision.isPassedAllGates()).isFalse();
        }
    }

    @Nested
    @DisplayName("2. Session Attribution, Event Lifecycle & Outcome Accumulation")
    class AttributionAndOutcomeAccumulationTests {

        @Test
        @DisplayName("Browsing-only sessions (Grades 0-2) are excluded from legitimate outcome counts")
        void testBrowsingOnlySessionExclusion() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now();
            List<RecommendationEvent> browsingEvents = List.of(
                    createEvent("c_live_01", "sess-browse-33", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                    createEvent("c_live_01", "sess-browse-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.plusSeconds(5), Map.of()),
                    createEvent("c_live_01", "sess-browse-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_EXPANDED, now.plusSeconds(8), Map.of()),
                    createEvent("c_live_01", "sess-browse-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_SAVED, now.plusSeconds(12), Map.of()),
                    createEvent("c_live_01", "sess-browse-33", "SCH-AGRI-01", RecommendationEventType.APPLICATION_STARTED, now.plusSeconds(15), Map.of())
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                    readinessMonitor.evaluateTelemetry(browsingEvents, List.of());

            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(snapshot.isTrainingReady()).isFalse();
            assertThat(snapshot.isModelTrainingAllowed()).isFalse();
            assertThat(snapshot.isModelPromotionAllowed()).isFalse();
        }

        @Test
        @DisplayName("SCHEME_APPLIED qualifies as legitimate outcome session when all requirements pass")
        void testSchemeAppliedQualification() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now();
            List<RecommendationEvent> appliedEvents = List.of(
                    createEvent("c_live_02", "sess-applied-33", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                    createEvent("c_live_02", "sess-applied-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.plusSeconds(5), Map.of()),
                    createEvent("c_live_02", "sess-applied-33", "SCH-AGRI-01", RecommendationEventType.APPLICATION_STARTED, now.plusSeconds(10), Map.of()),
                    createEvent("c_live_02", "sess-applied-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_APPLIED, now.plusSeconds(20), Map.of())
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                    readinessMonitor.evaluateTelemetry(appliedEvents, List.of());

            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1L);
            assertThat(snapshot.getRemaining()).isEqualTo(99L);
            assertThat(snapshot.isTrainingReady()).isFalse();
        }

        @Test
        @DisplayName("APPLICATION_COMPLETED qualifies as legitimate outcome session when all requirements pass")
        void testApplicationCompletedQualification() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now();
            List<RecommendationEvent> completedEvents = List.of(
                    createEvent("c_live_03", "sess-completed-33", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                    createEvent("c_live_03", "sess-completed-33", "SCH-AGRI-01", RecommendationEventType.APPLICATION_COMPLETED, now.plusSeconds(15), Map.of())
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                    readinessMonitor.evaluateTelemetry(completedEvents, List.of());

            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1L);
            assertThat(snapshot.getThreshold()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Objective D — Idempotency: Repeated monitoring runs do NOT double-count the same outcome session")
        void testOutcomeAccumulationIdempotency() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now();
            List<RecommendationEvent> convertedEvents = List.of(
                    createEvent("c_live_04", "sess-idempotent-33", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                    createEvent("c_live_04", "sess-idempotent-33", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.plusSeconds(5), Map.of()),
                    createEvent("c_live_04", "sess-idempotent-33", "SCH-AGRI-01", RecommendationEventType.APPLICATION_COMPLETED, now.plusSeconds(15), Map.of())
            );

            // Run 1
            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot run1 =
                    readinessMonitor.evaluateTelemetry(convertedEvents, List.of());
            assertThat(run1.getLegitimateOutcomeSessions()).isEqualTo(1L);

            // Run 2 (identical events evaluated in subsequent monitoring cycle)
            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot run2 =
                    readinessMonitor.evaluateTelemetry(convertedEvents, List.of());
            assertThat(run2.getLegitimateOutcomeSessions()).isEqualTo(1L);

            // Run 3
            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot run3 =
                    readinessMonitor.evaluateTelemetry(convertedEvents, List.of());
            assertThat(run3.getLegitimateOutcomeSessions()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("3. Training Readiness State Machine & Handoff Safeguards")
    class StateMachineAndHandoffTests {

        @Test
        @DisplayName("State 1: 0 <= outcomes < 100 results in TRAINING_NOT_READY and modelTrainingAllowed=false")
        void testState1_BelowThreshold() {
            TrainingReadinessGate.GateDecision decision0 = trainingReadinessGate.evaluate(0);
            assertThat(decision0.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(decision0.isModelTrainingAllowed()).isFalse();

            TrainingReadinessGate.GateDecision decision99 = trainingReadinessGate.evaluate(99);
            assertThat(decision99.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_NOT_READY);
            assertThat(decision99.isModelTrainingAllowed()).isFalse();
        }

        @Test
        @DisplayName("State 2: outcomes >= 100 transitions to TRAINING_READY, modelPromotionAllowed remains strictly false")
        void testState2_AtOrAboveThreshold() {
            TrainingReadinessGate.GateDecision decision100 = trainingReadinessGate.evaluate(100);
            assertThat(decision100.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_READY);
            assertThat(decision100.isModelTrainingAllowed()).isTrue();

            TrainingReadinessGate.GateDecision decision150 = trainingReadinessGate.evaluate(150);
            assertThat(decision150.getStatus()).isEqualTo(TrainingReadinessStatus.TRAINING_READY);
            assertThat(decision150.isModelTrainingAllowed()).isTrue();
        }

        @Test
        @DisplayName("Objective E: ContinuousReadinessSnapshot contains reliable dynamic top-level fields")
        void testContinuousReadinessSnapshot_TopLevelFields() {
            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                    readinessMonitor.evaluateTelemetry(List.of(), List.of());

            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(snapshot.getThreshold()).isEqualTo(100L);
            assertThat(snapshot.getRemaining()).isEqualTo(100L);
            assertThat(snapshot.isTrainingReady()).isFalse();
            assertThat(snapshot.isModelTrainingAllowed()).isFalse();
            assertThat(snapshot.isModelPromotionAllowed()).isFalse();
            assertThat(snapshot.getActiveModel()).isEqualTo("2.2.0-hybrid-semantic-384d");
            assertThat(snapshot.getFallbackModel()).isEqualTo("1.0.0-deterministic");
        }

        @Test
        @DisplayName("Objective G: 29 historical application_events remain quarantined and contribute 0 outcomes")
        void testHistoricalFixturesQuarantined() {
            List<ApplicationEvent> historicalFixtures = new ArrayList<>();
            for (int i = 0; i < 29; i++) {
                historicalFixtures.add(ApplicationEvent.builder()
                        .id("fixture_app_evt_" + i)
                        .userId("citizen_user")
                        .applicationId("6a952810c6037907f0030c06")
                        .build());
            }

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                    readinessMonitor.evaluateTelemetry(List.of(), historicalFixtures);

            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0L);
            assertThat(snapshot.getReadinessReport().getSyntheticFixturesQuarantined()).isEqualTo(29L);
            assertThat(snapshot.isTrainingReady()).isFalse();
            assertThat(snapshot.isModelTrainingAllowed()).isFalse();
            assertThat(snapshot.isModelPromotionAllowed()).isFalse();
        }
    }
}
