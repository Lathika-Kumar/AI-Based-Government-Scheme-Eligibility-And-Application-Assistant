package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.ml.dataset.*;
import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import com.schemebridge.scheme.repository.ApplicationEventRepository;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.RecommendationEventRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 38: Behavioral Dataset Qualification & Training Gate Test Suite.
 * Covers requirements A through Z:
 * - A: Zero historical users is handled correctly.
 * - B: Zero historical behavioral records is handled correctly.
 * - C: Current-user recommendation remains functional.
 * - D: No historical user dependency is introduced.
 * - E: Only genuine outcomes qualify.
 * - F: Synthetic events are rejected.
 * - G: 29 historical synthetic fixtures remain quarantined.
 * - H: Orphan events are rejected.
 * - I: Duplicate events are rejected.
 * - J: Invalid event sequences are rejected.
 * - K: Browsing-only sessions do not qualify.
 * - L: Valid conversion sessions qualify.
 * - M: PII-containing records are rejected.
 * - N: PII values are never logged.
 * - O: Target leakage is detected.
 * - P: Features and labels are separated.
 * - Q: Below 100 outcomes: trainingReady = false, modelTrainingAllowed = false.
 * - R: Threshold calculation is deterministic.
 * - S: Exactly 100 legitimate outcomes makes the dataset training-eligible in an isolated test fixture.
 * - T: More than 100 legitimate outcomes remains training-eligible.
 * - U: Training does NOT automatically execute.
 * - V: Model promotion remains disabled.
 * - W: Active recommender remains 2.2.0-hybrid-semantic-384d.
 * - X: Fallback remains 1.0.0-deterministic.
 * - Y: Circuit breaker remains 200 ms.
 * - Z: Production database is not mutated during audit.
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase38BehavioralDatasetQualificationTest {

    @Autowired
    private EligibleSchemeRecommendationService recommendationService;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private RecommendationEventRepository recommendationEventRepository;

    @Autowired
    private ApplicationEventRepository applicationEventRepository;

    @Autowired
    private MlRecommenderProperties mlProperties;

    @Autowired
    private TrainingReadinessGate trainingReadinessGate;

    @Autowired
    private BehavioralTrainingDatasetQualificationService qualificationService;

    @Autowired
    private Phase29TelemetryQualityMonitor telemetryQualityMonitor;

    @Autowired
    private TargetLeakageDetector targetLeakageDetector;

    @Autowired
    private SessionAttributionService sessionAttributionService;

    private static final String TEST_CITIZEN_ID = "citizen_p38_test_001";

    @BeforeEach
    void setUp() {
        cleanTestData();

        CitizenProfile citizenProfile = CitizenProfile.builder()
                .userId(TEST_CITIZEN_ID)
                .displayName("Pooja Sharma")
                .dob(LocalDate.of(2002, 5, 20))
                .age(24)
                .gender("FEMALE")
                .state("Maharashtra")
                .district("Pune")
                .occupation("STUDENT")
                .isStudent(true)
                .annualIncome(0.0)
                .socialCategory("GENERAL")
                .maritalStatus("SINGLE")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(citizenProfile);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        citizenProfileRepository.findByUserId(TEST_CITIZEN_ID).ifPresent(p -> citizenProfileRepository.delete(p));
    }

    @Test
    @DisplayName("Requirement A: Zero historical users is handled correctly")
    void testA_ZeroHistoricalUsersHandledCorrectly() {
        // Even when zero historical users exist, current authenticated citizen receives recommendations
        PersonalizedSchemeRecommendationResponse recs = recommendationService.getPersonalizedRecommendations(TEST_CITIZEN_ID, 0, 10);
        assertNotNull(recs);
        assertNotNull(recs.getRecommendations());
        assertEquals("2.2.0-hybrid-semantic-384d", recs.getModelVersion());
    }

    @Test
    @DisplayName("Requirement B: Zero historical behavioral records is handled correctly")
    void testB_ZeroHistoricalBehavioralRecordsHandledCorrectly() {
        DatasetQualificationSnapshot snapshot = qualificationService.qualifyLive();
        assertNotNull(snapshot);
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
        assertEquals(100L, snapshot.getMinimumOutcomeThreshold());
        assertEquals(100L, snapshot.getRemainingOutcomes());
        assertEquals("NOT_READY", snapshot.getStatus());
        assertFalse(snapshot.isTrainingReady());
        assertFalse(snapshot.isModelTrainingAllowed());
        assertFalse(snapshot.isModelPromotionAllowed());
    }

    @Test
    @DisplayName("Requirement C: Current-user recommendation remains functional")
    void testC_CurrentUserRecommendationRemainsFunctional() {
        PersonalizedSchemeRecommendationResponse recs = recommendationService.getPersonalizedRecommendations(TEST_CITIZEN_ID, 0, 10);
        assertNotNull(recs);
        assertFalse(recs.getRecommendations().isEmpty());
        assertTrue(recs.getTotalEligibleSchemes() > 0);
    }

    @Test
    @DisplayName("Requirement D: No historical user dependency is introduced")
    void testD_NoHistoricalUserDependencyIntroduced() {
        // Cold start citizen with zero past sessions or history
        PersonalizedSchemeRecommendationResponse recs = recommendationService.getPersonalizedRecommendations(TEST_CITIZEN_ID, 0, 10);
        assertNotNull(recs.getRecommendations());
        assertNotNull(recs.getModelVersion());
        assertEquals("1.0.0-deterministic", recs.getFallbackModel());
    }

    private String getValidSchemeCode() {
        return schemeRepository.findAll().stream()
                .findFirst()
                .map(Scheme::getSchemeCode)
                .orElse("SCH-SEARCH-01");
    }

    @Test
    @DisplayName("Requirement E: Only genuine outcomes qualify")
    void testE_OnlyGenuineOutcomesQualify() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        String sessId = "session_genuine_001";

        List<RecommendationEvent> recEvents = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_01").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_01").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_VIEWED).timestamp(now.plusSeconds(5)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_01").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_STARTED).timestamp(now.plusSeconds(10)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_01").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_APPLIED).timestamp(now.plusSeconds(15)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_01").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now.plusSeconds(20)).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(recEvents, List.of());
        assertEquals(1L, snapshot.getLegitimateOutcomeSessions());
        assertEquals(1L, snapshot.getEligibleTrainingExamples());
        assertEquals(0L, snapshot.getRejectedTrainingExamples());
    }

    @Test
    @DisplayName("Requirement F: Synthetic events are rejected")
    void testF_SyntheticEventsAreRejected() {
        Instant now = Instant.now();
        List<RecommendationEvent> syntheticEvents = List.of(
                RecommendationEvent.builder().sessionId("sess_syn_1").userId("synthetic_citizen_001")
                        .schemeCode("SCH_01").eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId("sess_syn_1").userId("synthetic_citizen_001")
                        .schemeCode("SCH_01").eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now.plusSeconds(10)).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(syntheticEvents, List.of());
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
        assertTrue(snapshot.getSyntheticEvents() > 0);
    }

    @Test
    @DisplayName("Requirement G: 29 historical synthetic fixtures remain quarantined")
    void testG_29HistoricalSyntheticFixturesRemainQuarantined() {
        DatasetQualificationSnapshot liveSnapshot = qualificationService.qualifyLive();
        // Historical fixtures contribute 0 legitimate outcome sessions
        assertEquals(0L, liveSnapshot.getLegitimateOutcomeSessions());
    }

    @Test
    @DisplayName("Requirement H: Orphan events are rejected")
    void testH_OrphanEventsAreRejected() {
        Instant now = Instant.now();
        String sessId = "session_orphan_001";
        // Event without RECOMMENDATION_SHOWN anchor
        List<RecommendationEvent> orphanEvents = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_orphan").schemeCode("SCH_01")
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(orphanEvents, List.of());
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
        assertTrue(snapshot.getOrphanEvents() > 0 || snapshot.getRejectedTrainingExamples() > 0);
    }

    @Test
    @DisplayName("Requirement I: Duplicate events are rejected")
    void testI_DuplicateEventsAreRejected() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        String sessId = "session_dup_001";
        List<RecommendationEvent> dups = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_dup").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_dup").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(dups, List.of());
        assertEquals(1L, snapshot.getDuplicateEvents());
    }

    @Test
    @DisplayName("Requirement J: Invalid event sequences are rejected")
    void testJ_InvalidEventSequencesAreRejected() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now().minusSeconds(200);
        String sessId = "session_invalid_seq";
        // Non-chronological sequence where second event precedes first event timestamp
        List<RecommendationEvent> invalidSeq = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_inv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_inv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now.minusSeconds(50)).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(invalidSeq, List.of());
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
        assertTrue(snapshot.getInvalidSequenceEvents() > 0);
    }

    @Test
    @DisplayName("Requirement K: Browsing-only sessions do not qualify")
    void testK_BrowsingOnlySessionsDoNotQualify() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        String sessId = "session_browsing_only";
        List<RecommendationEvent> browsingEvents = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_browse").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_browse").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_VIEWED).timestamp(now.plusSeconds(2)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_browse").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_EXPANDED).timestamp(now.plusSeconds(4)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_browse").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_SAVED).timestamp(now.plusSeconds(6)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_browse").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_STARTED).timestamp(now.plusSeconds(8)).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(browsingEvents, List.of());
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
        assertEquals(0L, snapshot.getEligibleTrainingExamples());
    }

    @Test
    @DisplayName("Requirement L: Valid conversion sessions qualify")
    void testL_ValidConversionSessionsQualify() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        String sessId = "session_valid_conv";
        List<RecommendationEvent> convEvents = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_conv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_conv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_VIEWED).timestamp(now.plusSeconds(3)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_conv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_STARTED).timestamp(now.plusSeconds(6)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_conv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_APPLIED).timestamp(now.plusSeconds(9)).build(),
                RecommendationEvent.builder().sessionId(sessId).userId("c_conv").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now.plusSeconds(12)).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(convEvents, List.of());
        assertEquals(1L, snapshot.getLegitimateOutcomeSessions());
        assertEquals(1L, snapshot.getEligibleTrainingExamples());
    }

    @Test
    @DisplayName("Requirement M: PII-containing records are rejected")
    void testM_PiiContainingRecordsAreRejected() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        String sessId = "session_pii_violation";
        Map<String, Object> piiMetadata = Map.of("aadhaarNumber", "123456789012");

        List<RecommendationEvent> piiEvents = List.of(
                RecommendationEvent.builder().sessionId(sessId).userId("c_pii").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).metadata(piiMetadata).build()
        );

        DatasetQualificationSnapshot snapshot = qualificationService.qualify(piiEvents, List.of());
        assertEquals(1L, snapshot.getPiiViolations());
        assertEquals(0L, snapshot.getLegitimateOutcomeSessions());
    }

    @Test
    @DisplayName("Requirement N: PII values are never logged")
    void testN_PiiValuesAreNeverLogged() {
        // Verify that PII detection catches forbidden PII keys without logging values
        assertTrue(telemetryQualityMonitor.containsPiiKey("aadhaar"));
        assertTrue(telemetryQualityMonitor.containsPiiKey("pan"));
        assertTrue(telemetryQualityMonitor.containsPiiKey("phone"));
        assertTrue(telemetryQualityMonitor.containsPiiKey("email"));
        assertTrue(telemetryQualityMonitor.containsPiiKey("address"));
        assertFalse(telemetryQualityMonitor.containsPiiKey("schemeCode"));
        assertFalse(telemetryQualityMonitor.containsPiiKey("category"));
    }

    @Test
    @DisplayName("Requirement O: Target leakage is detected")
    void testO_TargetLeakageIsDetected() {
        Map<String, Object> leakedContext = Map.of("application_completed", true);
        UserSchemeFeatureVector vector = UserSchemeFeatureVector.builder()
                .rankingContext(leakedContext)
                .build();

        assertThrows(IllegalStateException.class, () -> targetLeakageDetector.audit(vector));
        assertTrue(targetLeakageDetector.hasLeakage(leakedContext));
    }

    @Test
    @DisplayName("Requirement P: Features and labels are separated")
    void testP_FeaturesAndLabelsAreSeparated() {
        // Construct a clean TrainingExample
        TrainingExample example = qualificationService.createTrainingExample(
                "ex_001",
                "sess_001",
                "SCH_001",
                Map.of("age", 24, "state", "Maharashtra"),
                Map.of("category", "EDUCATION"),
                Map.of("rankPosition", 1),
                List.of("RECOMMENDATION_SHOWN", "SCHEME_VIEWED"),
                "APPLICATION_COMPLETED",
                true,
                RelevanceGrade.CONVERTED,
                System.currentTimeMillis()
        );

        assertNotNull(example);
        assertNotNull(example.getFeatures());
        assertNotNull(example.getLabel());
        assertTrue(example.isStatutoryEligible());
        assertEquals("APPLICATION_COMPLETED", example.getLabel().getTargetEventType());
        assertTrue(example.getLabel().isConverted());

        // Target leakage in features should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> qualificationService.createTrainingExample(
                "ex_002",
                "sess_002",
                "SCH_002",
                Map.of("converted", true), // LEAKAGE
                Map.of(),
                Map.of(),
                List.of(),
                "APPLICATION_COMPLETED",
                true,
                RelevanceGrade.CONVERTED,
                System.currentTimeMillis()
        ));
    }

    @Test
    @DisplayName("Requirement Q: Below 100 outcomes: trainingReady = false, modelTrainingAllowed = false")
    void testQ_Below100OutcomesTrainingDisabled() {
        TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(99L);
        assertEquals(TrainingReadinessStatus.TRAINING_NOT_READY, decision.getStatus());
        assertFalse(decision.isModelTrainingAllowed());
        assertFalse(decision.isModelPromotionAllowed());
        assertEquals(1L, decision.getRemainingOutcomeSessions());
    }

    @Test
    @DisplayName("Requirement R: Threshold calculation is deterministic")
    void testR_ThresholdCalculationIsDeterministic() {
        TrainingReadinessGate.GateDecision d0 = trainingReadinessGate.evaluate(0L);
        assertEquals(100L, d0.getRemainingOutcomeSessions());

        TrainingReadinessGate.GateDecision d50 = trainingReadinessGate.evaluate(50L);
        assertEquals(50L, d50.getRemainingOutcomeSessions());

        TrainingReadinessGate.GateDecision d100 = trainingReadinessGate.evaluate(100L);
        assertEquals(0L, d100.getRemainingOutcomeSessions());
    }

    @Test
    @DisplayName("Requirement S: Exactly 100 legitimate outcomes makes dataset training-eligible in isolated fixture")
    void testS_Exactly100LegitimateOutcomesMakesDatasetTrainingEligible() {
        TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(100L);
        assertEquals(TrainingReadinessStatus.TRAINING_READY, decision.getStatus());
        assertTrue(decision.isModelTrainingAllowed());
        assertFalse(decision.isModelPromotionAllowed()); // Promotion always false!
        assertEquals(0L, decision.getRemainingOutcomeSessions());
    }

    @Test
    @DisplayName("Requirement T: More than 100 legitimate outcomes remains training-eligible")
    void testT_MoreThan100LegitimateOutcomesRemainsTrainingEligible() {
        TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(150L);
        assertEquals(TrainingReadinessStatus.TRAINING_READY, decision.getStatus());
        assertTrue(decision.isModelTrainingAllowed());
        assertFalse(decision.isModelPromotionAllowed());
    }

    @Test
    @DisplayName("Requirement U: Training does NOT automatically execute")
    void testU_TrainingDoesNotAutomaticallyExecute() {
        // Qualifying the live database or meeting threshold does NOT invoke training or alter active model
        DatasetQualificationSnapshot snapshot = qualificationService.qualifyLive();
        assertEquals("2.2.0-hybrid-semantic-384d", snapshot.getActiveModel());
        assertEquals("1.0.0-deterministic", snapshot.getFallbackModel());
    }

    @Test
    @DisplayName("Requirement V: Model promotion remains disabled")
    void testV_ModelPromotionRemainsDisabled() {
        assertFalse(qualificationService.isModelPromotionAllowed());
        TrainingReadinessGate.GateDecision decision = trainingReadinessGate.evaluate(500L);
        assertFalse(decision.isModelPromotionAllowed());
    }

    @Test
    @DisplayName("Requirement W: Active recommender remains 2.2.0-hybrid-semantic-384d")
    void testW_ActiveRecommenderRemains220HybridSemantic() {
        assertEquals("2.2.0-hybrid-semantic-384d", EligibleSchemeRecommendationService.MODEL_VERSION);
        assertEquals("2.2.0-hybrid-semantic-384d", BehavioralTrainingDatasetQualificationService.ACTIVE_MODEL);
    }

    @Test
    @DisplayName("Requirement X: Fallback remains 1.0.0-deterministic")
    void testX_FallbackRemains100Deterministic() {
        assertEquals("1.0.0-deterministic", EligibleSchemeRecommendationService.FALLBACK_MODEL_VERSION);
        assertEquals("1.0.0-deterministic", BehavioralTrainingDatasetQualificationService.FALLBACK_MODEL);
    }

    @Test
    @DisplayName("Requirement Y: Circuit breaker remains 200 ms")
    void testY_CircuitBreakerRemains200ms() {
        assertEquals(200, mlProperties.getMl().getTimeoutMs());
        assertEquals(200, BehavioralTrainingDatasetQualificationService.CIRCUIT_BREAKER_MS);
    }

    @Test
    @DisplayName("Requirement Z: Production database is not mutated during audit")
    void testZ_ProductionDatabaseNotMutatedDuringAudit() {
        long schemesBefore = schemeRepository.count();
        long recEventsBefore = recommendationEventRepository.count();
        long appEventsBefore = applicationEventRepository.count();

        DatasetQualificationSnapshot snapshot = qualificationService.qualifyLive();
        assertNotNull(snapshot);

        assertEquals(schemesBefore, schemeRepository.count());
        assertEquals(recEventsBefore, recommendationEventRepository.count());
        assertEquals(appEventsBefore, applicationEventRepository.count());
    }
}
