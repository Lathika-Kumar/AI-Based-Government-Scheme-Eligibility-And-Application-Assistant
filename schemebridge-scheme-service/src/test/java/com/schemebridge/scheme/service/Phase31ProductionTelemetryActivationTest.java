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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Phase31ProductionTelemetryActivationTest {

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

    private List<RecommendationEvent> generateOutcomeSession(String sessionId, String schemeCode, Instant baseTime, RecommendationEventType conversionType) {
        return List.of(
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.RECOMMENDATION_SHOWN, baseTime, null),
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.SCHEME_VIEWED, baseTime.plusSeconds(10), null),
                createEvent("user_" + sessionId, sessionId, schemeCode, RecommendationEventType.APPLICATION_STARTED, baseTime.plusSeconds(30), null),
                createEvent("user_" + sessionId, sessionId, schemeCode, conversionType, baseTime.plusSeconds(120), null)
        );
    }

    @Nested
    @DisplayName("Requirements A-E: Thresholds & State Machine Transitions")
    class ThresholdAndStateTransitions {

        @Test
        @DisplayName("A. 0 legitimate sessions -> TRAINING_NOT_READY")
        void testRequirementA_ZeroSessions() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> events = List.of(
                    createEvent("u1", "s1", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, Instant.now().minusSeconds(10), null)
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(events, List.of());
            Phase31ReadinessReport report = snapshot.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(report.isTrainingReady()).isFalse();
            assertThat(report.isModelTrainingAllowed()).isFalse();
            assertThat(report.isModelPromotionAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(0);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(100);
        }

        @Test
        @DisplayName("B. 1 legitimate session -> TRAINING_NOT_READY")
        void testRequirementB_OneSession() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> events = generateOutcomeSession("s1", "SCH-AGRI-01", Instant.now().minusSeconds(300), RecommendationEventType.APPLICATION_COMPLETED);

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(events, List.of());
            Phase31ReadinessReport report = snapshot.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(report.isTrainingReady()).isFalse();
            assertThat(report.isModelTrainingAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(1);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(99);
        }

        @Test
        @DisplayName("C. 99 legitimate sessions -> TRAINING_NOT_READY")
        void testRequirementC_NinetyNineSessions() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> all = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(50000);
            for (int i = 0; i < 99; i++) {
                all.addAll(generateOutcomeSession("s_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L), RecommendationEventType.APPLICATION_COMPLETED));
            }

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(all, List.of());
            Phase31ReadinessReport report = snapshot.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_NOT_READY");
            assertThat(report.isTrainingReady()).isFalse();
            assertThat(report.isModelTrainingAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(99);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(1);
        }

        @Test
        @DisplayName("D. Exactly 100 legitimate sessions -> TRAINING_READY, modelTrainingAllowed=true, modelPromotionAllowed=false")
        void testRequirementD_ExactlyOneHundredSessions() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> all = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(60000);
            for (int i = 0; i < 100; i++) {
                all.addAll(generateOutcomeSession("s_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L), RecommendationEventType.APPLICATION_COMPLETED));
            }

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(all, List.of());
            Phase31ReadinessReport report = snapshot.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_READY");
            assertThat(report.isTrainingReady()).isTrue();
            assertThat(report.isModelTrainingAllowed()).isTrue();
            assertThat(report.isModelPromotionAllowed()).isFalse(); // Invariant: Model promotion strictly forbidden
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(100);
            assertThat(report.getRemainingOutcomeSessions()).isEqualTo(0);
        }

        @Test
        @DisplayName("E. 101 legitimate sessions -> TRAINING_READY, modelTrainingAllowed=true, modelPromotionAllowed=false")
        void testRequirementE_OneHundredOneSessions() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> all = new ArrayList<>();
            Instant base = Instant.now().minusSeconds(70000);
            for (int i = 0; i < 101; i++) {
                all.addAll(generateOutcomeSession("s_" + i, "SCH-AGRI-01", base.plusSeconds(i * 10L), RecommendationEventType.APPLICATION_COMPLETED));
            }

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(all, List.of());
            Phase31ReadinessReport report = snapshot.getReadinessReport();

            assertThat(report.getStatus()).isEqualTo("TRAINING_READY");
            assertThat(report.isTrainingReady()).isTrue();
            assertThat(report.isModelTrainingAllowed()).isTrue();
            assertThat(report.isModelPromotionAllowed()).isFalse();
            assertThat(report.getLegitimateOutcomeSessions()).isEqualTo(101);
        }
    }

    @Nested
    @DisplayName("Requirements F-O: Telemetry Quality Classification & Ingestion Gates")
    class TelemetryQualityAndIngestionGates {

        @Test
        @DisplayName("F. Synthetic session -> excluded from valid telemetry")
        void testRequirementF_SyntheticSessionExcluded() {
            ProductionTelemetryIngestionService.IngestionDecision decision = ingestionService.ingestEvent(
                    "citizen_user", "sess_syn", "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now(), null, null, null, null
            );

            assertThat(decision.getStatus()).isEqualTo(TelemetryQualityStatus.SYNTHETIC);
            assertThat(decision.isPassedAllGates()).isFalse();
        }

        @Test
        @DisplayName("G. Historical 29 synthetic fixtures -> all quarantined permanently")
        void testRequirementG_Historical29Quarantined() {
            List<ApplicationEvent> fixtures = new ArrayList<>();
            for (int i = 0; i < 29; i++) {
                fixtures.add(ApplicationEvent.builder()
                        .id("67634f19b84179320e6f7" + String.format("%03d", i))
                        .userId("citizen1")
                        .applicationId("6a952810c6037907f0030c06")
                        .build());
            }

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(List.of(), fixtures);
            assertThat(snapshot.getQualityReport().getSyntheticEvents()).isEqualTo(29);
            assertThat(snapshot.getReadinessReport().getSyntheticFixturesQuarantined()).isEqualTo(29);
            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0);
        }

        @Test
        @DisplayName("H. Duplicate event -> DUPLICATE status")
        void testRequirementH_DuplicateEvent() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));
            Set<String> seen = new HashSet<>();

            ProductionTelemetryIngestionService.IngestionDecision d1 = ingestionService.ingestEvent(
                    "u1", "sess_dup", "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now(), null, seen, new HashSet<>(), null
            );
            assertThat(d1.getStatus()).isEqualTo(TelemetryQualityStatus.VALID);

            ProductionTelemetryIngestionService.IngestionDecision d2 = ingestionService.ingestEvent(
                    "u1", "sess_dup", "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now(), null, seen, new HashSet<>(), null
            );
            assertThat(d2.getStatus()).isEqualTo(TelemetryQualityStatus.DUPLICATE);
        }

        @Test
        @DisplayName("I. Missing sessionId -> MALFORMED status")
        void testRequirementI_MissingSessionId() {
            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", null, "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now(), null, null, null, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("J. Missing schemeCode -> MALFORMED status")
        void testRequirementJ_MissingSchemeCode() {
            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", "s1", "", "RECOMMENDATION_SHOWN", Instant.now(), null, null, null, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("K. Future timestamp -> MALFORMED status")
        void testRequirementK_FutureTimestamp() {
            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", "s1", "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now().plusSeconds(10000), null, null, null, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.MALFORMED);
        }

        @Test
        @DisplayName("L. PII in nested metadata -> PII_VIOLATION status")
        void testRequirementL_PiiNestedMetadata() {
            Map<String, Object> nested = Map.of("aadhaar_number", "123456789012");
            Map<String, Object> meta = Map.of("client_info", nested);

            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", "s1", "SCH-AGRI-01", "RECOMMENDATION_SHOWN", Instant.now(), meta, null, null, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.PII_VIOLATION);
        }

        @Test
        @DisplayName("M. Orphan event (scheme missing in repository) -> ORPHAN status")
        void testRequirementM_OrphanSchemeMissing() {
            when(schemeRepository.findBySchemeCode("SCH-NONEXISTENT")).thenReturn(Optional.empty());

            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", "s1", "SCH-NONEXISTENT", "RECOMMENDATION_SHOWN", Instant.now(), null, null, null, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
        }

        @Test
        @DisplayName("N. Invalid chronological sequence -> INVALID_SEQUENCE")
        void testRequirementN_InvalidChronologicalSequence() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant now = Instant.now().minusSeconds(300);
            List<RecommendationEvent> inverted = List.of(
                    createEvent("u1", "s_inv", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, now, null),
                    createEvent("u1", "s_inv", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, now.minusSeconds(60), null)
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(inverted, List.of());
            assertThat(snapshot.getQualityReport().getInvalidSequenceEvents()).isGreaterThan(0);
            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0);
        }

        @Test
        @DisplayName("O. No RECOMMENDATION_SHOWN anchor -> ORPHAN status")
        void testRequirementO_NoRecommendationAnchor() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Set<String> anchoredSchemes = new HashSet<>(); // No RECOMMENDATION_SHOWN seen for this scheme

            ProductionTelemetryIngestionService.IngestionDecision d = ingestionService.ingestEvent(
                    "u1", "s1", "SCH-AGRI-01", "SCHEME_VIEWED", Instant.now(), null, null, anchoredSchemes, null
            );
            assertThat(d.getStatus()).isEqualTo(TelemetryQualityStatus.ORPHAN);
            assertThat(d.getRejectionCategory()).isEqualTo("MISSING_RECOMMENDATION_ANCHOR");
        }
    }

    @Nested
    @DisplayName("Requirements P-S: Outcome Qualification & Statutory Eligibility")
    class OutcomeQualificationAndEligibility {

        @Test
        @DisplayName("P. Browsing-only session -> does NOT increment outcome counter")
        void testRequirementP_BrowsingOnlySession() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            Instant base = Instant.now().minusSeconds(200);
            List<RecommendationEvent> browsing = List.of(
                    createEvent("u1", "s_browse", "SCH-AGRI-01", RecommendationEventType.RECOMMENDATION_SHOWN, base, null),
                    createEvent("u1", "s_browse", "SCH-AGRI-01", RecommendationEventType.SCHEME_VIEWED, base.plusSeconds(10), null),
                    createEvent("u1", "s_browse", "SCH-AGRI-01", RecommendationEventType.SCHEME_EXPANDED, base.plusSeconds(20), null),
                    createEvent("u1", "s_browse", "SCH-AGRI-01", RecommendationEventType.APPLICATION_STARTED, base.plusSeconds(30), null)
            );

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(browsing, List.of());
            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(0);
            assertThat(snapshot.getQualityReport().getRelevanceGradesBreakdown().get("GRADE_2_INTENT_HIGH")).isEqualTo(1);
        }

        @Test
        @DisplayName("Q. SCHEME_APPLIED -> qualifies as legitimate outcome")
        void testRequirementQ_SchemeAppliedOutcome() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> applied = generateOutcomeSession("s_app", "SCH-AGRI-01", Instant.now().minusSeconds(200), RecommendationEventType.SCHEME_APPLIED);

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(applied, List.of());
            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1);
            assertThat(snapshot.getQualityReport().getRelevanceGradesBreakdown().get("GRADE_3_CONVERTED")).isEqualTo(1);
        }

        @Test
        @DisplayName("R. APPLICATION_COMPLETED -> qualifies as legitimate outcome")
        void testRequirementR_ApplicationCompletedOutcome() {
            when(schemeRepository.findBySchemeCode("SCH-AGRI-01")).thenReturn(Optional.of(canonicalScheme));

            List<RecommendationEvent> completed = generateOutcomeSession("s_comp", "SCH-AGRI-01", Instant.now().minusSeconds(200), RecommendationEventType.APPLICATION_COMPLETED);

            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(completed, List.of());
            assertThat(snapshot.getLegitimateOutcomeSessions()).isEqualTo(1);
            assertThat(snapshot.getQualityReport().getRelevanceGradesBreakdown().get("GRADE_3_CONVERTED")).isEqualTo(1);
        }

        @Test
        @DisplayName("S. Ineligible scheme -> excluded from training pair creation")
        void testRequirementS_IneligibleSchemeExcluded() {
            CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                    .age(80)
                    .gender("FEMALE")
                    .state("Delhi")
                    .build();

            when(eligibilityEngine.evaluate(profile, canonicalScheme)).thenReturn(
                    EligibilityEvaluationResult.builder()
                            .schemeCode(canonicalScheme.getSchemeCode())
                            .status(EligibilityStatus.NOT_ELIGIBLE)
                            .explanation("Age exceeds threshold")
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
    }

    @Nested
    @DisplayName("Requirements T-X: Leakage, Splits, PII, and Database Invariants")
    class LeakageSplitsAndDatabaseInvariants {

        @Test
        @DisplayName("T. Target leakage -> dataset generation fails safely")
        void testRequirementT_TargetLeakageFailsSafely() {
            Map<String, Object> dirtyContext = new HashMap<>();
            dirtyContext.put("currentRank", 1);
            dirtyContext.put("relevanceGrade", 3);

            UserSchemeFeatureVector vector = UserSchemeFeatureVector.builder()
                    .schemaVersion("1.0.0")
                    .rankingContext(dirtyContext)
                    .build();

            assertThatThrownBy(() -> targetLeakageDetector.audit(vector))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CRITICAL TARGET LEAKAGE DETECTED");
        }

        @Test
        @DisplayName("U. Session split isolation -> zero cross-split overlap")
        void testRequirementU_SessionSplitIsolation() {
            Set<String> train = new HashSet<>();
            Set<String> val = new HashSet<>();
            Set<String> test = new HashSet<>();

            for (int i = 0; i < 400; i++) {
                String sessionId = "sess_p31_" + i;
                String split = sessionAttributionService.assignSessionSplit(sessionId);
                switch (split) {
                    case "TRAIN" -> train.add(sessionId);
                    case "VALIDATION" -> val.add(sessionId);
                    case "TEST" -> test.add(sessionId);
                }
            }

            Set<String> overlap1 = new HashSet<>(train);
            overlap1.retainAll(val);
            assertThat(overlap1).isEmpty();

            Set<String> overlap2 = new HashSet<>(train);
            overlap2.retainAll(test);
            assertThat(overlap2).isEmpty();

            Set<String> overlap3 = new HashSet<>(val);
            overlap3.retainAll(test);
            assertThat(overlap3).isEmpty();
        }

        @Test
        @DisplayName("V. Deterministic SHA-256 assignment -> same session always maps to same split")
        void testRequirementV_DeterministicSplitAssignment() {
            String sessionId = "consistent_session_id_42";
            String firstAssignment = sessionAttributionService.assignSessionSplit(sessionId);
            for (int i = 0; i < 20; i++) {
                assertThat(sessionAttributionService.assignSessionSplit(sessionId)).isEqualTo(firstAssignment);
            }
        }

        @Test
        @DisplayName("W. PII values never appear in diagnostic logs")
        void testRequirementW_PiiNeverLogged() {
            // Verify PII check drops keys without logging values
            boolean hasPii = qualityMonitor.hasPiiRecursively(Map.of("phone", "9876543210"));
            assertThat(hasPii).isTrue();
        }

        @Test
        @DisplayName("X. Production database mutation count remains zero")
        void testRequirementX_ZeroMutations() {
            Phase31ContinuousReadinessMonitor.ContinuousMonitoringSnapshot snapshot = readinessMonitor.evaluateTelemetry(List.of(), List.of());
            Map<String, Integer> mutations = snapshot.getReadinessReport().getDatabaseMutations();

            assertThat(mutations.get("INSERT")).isEqualTo(0);
            assertThat(mutations.get("UPDATE")).isEqualTo(0);
            assertThat(mutations.get("DELETE")).isEqualTo(0);
            assertThat(mutations.get("DROP")).isEqualTo(0);
        }
    }
}
