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
 * Phase 32: Production Telemetry Integration & Real Outcome Accumulation Test Suite.
 * Validates the end-to-end integration between genuine citizen interaction flow,
 * the 11-gate ingestion boundary, 7-way classification precedence, statutory eligibility authority,
 * monotonic terminal grade resolution, and continuous readiness monitoring.
 */
@ExtendWith(MockitoExtension.class)
class Phase32ProductionTelemetryIntegrationTest {

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

    @Test
    @DisplayName("Gate 1 & Gate 4: Malformed events with missing required fields are classified as MALFORMED")
    void testMalformedEvents_Rejected() {
        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_100", "", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.MALFORMED);
        assertThat(decision.getRejectionCategory()).isEqualTo("MISSING_REQUIRED_FIELDS");
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 6: Synthetic fixtures are detected and classified as SYNTHETIC")
    void testSyntheticEvents_Quarantined() {
        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_user", "sess-synthetic-01", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.SYNTHETIC);
        assertThat(decision.getRejectionCategory()).isEqualTo("SYNTHETIC_FIXTURE");
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 7: PII keys in metadata are classified as PII_VIOLATION without logging values")
    void testPiiViolations_QuarantinedWithoutLoggingValues() {
        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        Map<String, Object> piiMeta = Map.of("aadhaarNumber", "1234-5678-9012");
        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-pii-01", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), piiMeta, seen, anchors, null
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.PII_VIOLATION);
        assertThat(decision.getRejectionCategory()).isEqualTo("PII_KEY_DETECTED");
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 8: Non-existent schemes in master repository are classified as ORPHAN")
    void testOrphanEvents_Rejected() {
        when(schemeRepository.findBySchemeCode("SCH-NONEXISTENT")).thenReturn(Optional.empty());

        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-orphan-01", "SCH-NONEXISTENT", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
        assertThat(decision.getRejectionCategory()).isEqualTo("SCHEME_NOT_FOUND");
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 9: Non-anchor interaction event without preceding RECOMMENDATION_SHOWN is classified as ORPHAN")
    void testMissingRecommendationAnchor_Rejected() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>(); // empty anchor set for session

        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-no-anchor-01", "SCH-AGRI-01", "SCHEME_VIEWED",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
        assertThat(decision.getRejectionCategory()).isEqualTo("MISSING_RECOMMENDATION_ANCHOR");
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 10: Duplicate events in session are classified as DUPLICATE")
    void testDuplicateEvents_Rejected() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        ProductionTelemetryIngestionService.IngestionDecision first = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-dup-01", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(first.getStatus()).isEqualTo(TelemetryQualityStatus.VALID);

        ProductionTelemetryIngestionService.IngestionDecision duplicate = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-dup-01", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, null
        );
        assertThat(duplicate.getStatus()).isEqualTo(TelemetryQualityStatus.DUPLICATE);
        assertThat(duplicate.getRejectionCategory()).isEqualTo("DUPLICATE_IN_SESSION");
        assertThat(duplicate.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Gate 11: EligibilityEngine remains sole statutory eligibility authority")
    void testStatutoryEligibilityEngineAuthority() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        CitizenEligibilityProfile ineligibleProfile = CitizenEligibilityProfile.builder()
                .age(15) // Ineligible for adult scheme
                .annualIncome(9999999.0)
                .build();

        EligibilityEvaluationResult ineligibleResult = EligibilityEvaluationResult.builder()
                .status(EligibilityStatus.NOT_ELIGIBLE)
                .build();

        when(eligibilityEngine.evaluate(eq(ineligibleProfile), any(Scheme.class)))
                .thenReturn(ineligibleResult);

        Set<String> seen = new HashSet<>();
        Set<String> anchors = new HashSet<>();

        ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                "citizen_genuine_01", "sess-elig-01", "SCH-AGRI-01", "RECOMMENDATION_SHOWN",
                Instant.now(), Map.of(), seen, anchors, ineligibleProfile
        );
        assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.VALID);
        assertThat(decision.isEligible()).isFalse();
        assertThat(decision.isPassedAllGates()).isFalse();
    }

    @Test
    @DisplayName("Browsing-only sessions (Grades 0-2) do NOT increment legitimate outcomes")
    void testBrowsingOnlySessions_DoNotIncrementOutcomes() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        Instant now = Instant.now();
        List<RecommendationEvent> browsingEvents = List.of(
                createEvent("c_01", "sess-browse-01", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                createEvent("c_01", "sess-browse-01", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.plusSeconds(5), Map.of()),
                createEvent("c_01", "sess-browse-01", "SCH-AGRI-01", RecommendationEventType.APPLICATION_STARTED, now.plusSeconds(10), Map.of())
        );

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                readinessMonitor.evaluateTelemetry(browsingEvents, List.of());

        assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0L);
        assertThat(snapshot.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
        assertThat(snapshot.getReadinessReport().isTrainingReady()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelTrainingAllowed()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelPromotionAllowed()).isFalse();
    }

    @Test
    @DisplayName("SCHEME_APPLIED qualifies as legitimate outcome session when all 11 gates pass")
    void testSchemeApplied_QualifiesAsOutcome() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        Instant now = Instant.now();
        List<RecommendationEvent> appliedEvents = List.of(
                createEvent("c_02", "sess-applied-01", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                createEvent("c_02", "sess-applied-01", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.plusSeconds(5), Map.of()),
                createEvent("c_02", "sess-applied-01", "SCH-AGRI-01", RecommendationEventType.SCHEME_APPLIED, now.plusSeconds(10), Map.of())
        );

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                readinessMonitor.evaluateTelemetry(appliedEvents, List.of());

        assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1L);
        assertThat(snapshot.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
        assertThat(snapshot.getReadinessReport().getLegitimateOutcomeSessions()).isEqualTo(1L);
        assertThat(snapshot.getReadinessReport().getRemainingOutcomeSessions()).isEqualTo(99L);
        assertThat(snapshot.getReadinessReport().isTrainingReady()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelTrainingAllowed()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelPromotionAllowed()).isFalse();
    }

    @Test
    @DisplayName("APPLICATION_COMPLETED qualifies as legitimate outcome session when all 11 gates pass")
    void testApplicationCompleted_QualifiesAsOutcome() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        Instant now = Instant.now();
        List<RecommendationEvent> completedEvents = List.of(
                createEvent("c_03", "sess-completed-01", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                createEvent("c_03", "sess-completed-01", "SCH-AGRI-01", RecommendationEventType.APPLICATION_COMPLETED, now.plusSeconds(15), Map.of())
        );

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                readinessMonitor.evaluateTelemetry(completedEvents, List.of());

        assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1L);
        assertThat(snapshot.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
    }

    @Test
    @DisplayName("Invalid event sequence is rejected from outcome attribution")
    void testInvalidSequence_RejectedFromOutcomes() {
        when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

        // Interaction event with timestamp earlier than anchor event
        Instant now = Instant.now();
        List<RecommendationEvent> invalidSeq = List.of(
                createEvent("c_04", "sess-inv-seq-01", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, Map.of()),
                createEvent("c_04", "sess-inv-seq-01", "SCH-AGRI-01", RecommendationEventType.SCHEME_APPLIED, now.minusSeconds(60), Map.of())
        );

        Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot =
                readinessMonitor.evaluateTelemetry(invalidSeq, List.of());

        assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0L);
        assertThat(snapshot.getQualityReport().getInvalidSequenceEvents()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Zero real citizen events results in exact 0/100 legitimate outcomes and TRAINING_NOT_READY")
    void testZeroRealEvents_MaintainsZeroOutcomesAndTrainingNotReady() {
        // Historical 29 fixtures in applicationEventRepository
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
        assertThat(snapshot.getReadinessReport().getLegitimateOutcomeSessions()).isEqualTo(0L);
        assertThat(snapshot.getReadinessReport().getRequiredThreshold()).isEqualTo(100L);
        assertThat(snapshot.getReadinessReport().getRemainingOutcomeSessions()).isEqualTo(100L);
        assertThat(snapshot.getReadinessReport().getStatus()).isEqualTo("TRAINING_NOT_READY");
        assertThat(snapshot.getReadinessReport().isTrainingReady()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelTrainingAllowed()).isFalse();
        assertThat(snapshot.getReadinessReport().isModelPromotionAllowed()).isFalse();
        assertThat(snapshot.getReadinessReport().getSyntheticFixturesQuarantined()).isEqualTo(29L);
        assertThat(snapshot.getReadinessReport().getActiveModel()).isEqualTo("2.2.0-hybrid-semantic-384d");
        assertThat(snapshot.getReadinessReport().getFallbackModel()).isEqualTo("1.0.0-deterministic");
        assertThat(snapshot.getReadinessReport().getCircuitBreakerMs()).isEqualTo(200);
    }
}
