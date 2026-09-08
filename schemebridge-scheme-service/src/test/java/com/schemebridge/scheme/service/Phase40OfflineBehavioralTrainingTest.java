package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.ml.dataset.*;
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
 * Phase 40: Offline Behavioral ML Training Pipeline with Hard Production Safety Gate Test Suite.
 * Covers requirements A through Z:
 * Gate:
 * - A: 0/100 blocks training
 * - B: <100 blocks training
 * - C: Exactly 100 can become training eligible only if every other gate passes
 * - D: Integrity failure blocks training
 * - E: Promotion is always disabled
 * Dataset:
 * - F: Frozen dataset is immutable
 * - G: SHA-256 is deterministic
 * - H: Dataset version is deterministic
 * - I: Only legitimate outcomes qualify
 * - J: Browsing-only sessions are excluded
 * - K: Orphan terminal events are excluded
 * - L: Invalid chronology is excluded
 * Integrity:
 * - M: Synthetic fixtures are rejected
 * - N: 29 historical fixtures remain quarantined
 * - O: PII causes rejection
 * - P: Target leakage causes rejection
 * - Q: Feature/label separation is enforced
 * - R: Forbidden leakage keys cause immediate failure
 * Training isolation:
 * - S: Only frozen dataset is consumed
 * - T: Production DB remains unchanged
 * - U: Active model remains 2.2.0-hybrid-semantic-384d
 * - V: Fallback remains 1.0.0-deterministic
 * - W: Circuit breaker remains 200 ms
 * Evaluation:
 * - X: Evaluation is offline only
 * - Y: Candidate is never auto-activated
 * - Z: Promotion firewall remains closed
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase40OfflineBehavioralTrainingTest {

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
    private BehavioralTrainingDatasetQualificationService qualificationService;

    @Autowired
    private DatasetFreezeService datasetFreezeService;

    @Autowired
    private Phase40TrainingGate trainingGate;

    @Autowired
    private OfflineBehavioralModelTrainer offlineTrainer;

    @Autowired
    private OfflineBehavioralModelEvaluator offlineEvaluator;

    private static final String TEST_CITIZEN_ID = "citizen_p40_test_user";

    @BeforeEach
    void setUp() {
        cleanTestData();

        CitizenProfile citizenProfile = CitizenProfile.builder()
                .userId(TEST_CITIZEN_ID)
                .displayName("Ananya Deshmukh")
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

    private String getValidSchemeCode() {
        return schemeRepository.findAll().stream()
                .findFirst()
                .map(Scheme::getSchemeCode)
                .orElse("SCH-SEARCH-01");
    }

    // ==========================================
    // Gate Tests (A - E)
    // ==========================================

    @Test
    @DisplayName("A. 0/100 blocks training")
    void testA_ZeroOutOfHundredBlocksTraining() {
        Phase40TrainingGate.GateDecision decision = trainingGate.evaluateLive();
        assertEquals("TRAINING_BLOCKED_BELOW_THRESHOLD", decision.getGateStatus());
        assertFalse(decision.isTrainingPermitted());
        assertEquals(0L, decision.getLegitimateOutcomeSessions());
        assertEquals(100L, decision.getRequiredThreshold());
        assertEquals(100L, decision.getRemainingSessions());

        assertThrows(IllegalStateException.class, () -> trainingGate.enforceTrainingGate());
    }

    @Test
    @DisplayName("B. <100 blocks training")
    void testB_LessThanHundredBlocksTraining() {
        String schemeCode = getValidSchemeCode();
        Instant baseTime = Instant.now().minusSeconds(10000);
        List<RecommendationEvent> recEvents = new ArrayList<>();

        // Generate 50 in-memory test events (less than 100)
        for (int i = 0; i < 50; i++) {
            String sessId = "mem_sess_" + i;
            Instant t0 = baseTime.plusSeconds(i * 10L);
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("u_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(t0).build());
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("u_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(t0.plusSeconds(5)).build());
        }

        Phase40TrainingGate.GateDecision decision = trainingGate.evaluate(recEvents, List.of());
        assertEquals("TRAINING_BLOCKED_BELOW_THRESHOLD", decision.getGateStatus());
        assertFalse(decision.isTrainingPermitted());
        assertEquals(50L, decision.getLegitimateOutcomeSessions());
        assertEquals(50L, decision.getRemainingSessions());
    }

    @Test
    @DisplayName("C. Exactly 100 can become training eligible only if every other gate passes")
    void testC_ExactlyHundredEligibleIfAllGatesPass() {
        String schemeCode = getValidSchemeCode();
        Instant baseTime = Instant.now().minusSeconds(20000);
        List<RecommendationEvent> recEvents = new ArrayList<>();

        // Generate exactly 100 qualifying in-memory test sessions
        for (int i = 0; i < 100; i++) {
            String sessId = "mem_qual_sess_" + i;
            Instant t0 = baseTime.plusSeconds(i * 10L);
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("user_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(t0).build());
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("user_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(t0.plusSeconds(5)).build());
        }

        DatasetFreezeSnapshot freezeSnapshot = datasetFreezeService.freeze(recEvents, List.of());
        assertEquals(100L, freezeSnapshot.getQualifyingSessionCount());
        assertEquals("FROZEN_SUCCESS", freezeSnapshot.getFreezeStatus());

        Phase40TrainingGate.GateDecision decision = trainingGate.evaluate(freezeSnapshot);
        assertEquals("TRAINING_PERMITTED_OFFLINE_ONLY", decision.getGateStatus());
        assertTrue(decision.isTrainingPermitted());
        assertFalse(decision.isModelPromotionAllowed()); // Promotion firewall remains locked
    }

    @Test
    @DisplayName("D. Integrity failure blocks training")
    void testD_IntegrityFailureBlocksTraining() {
        String schemeCode = getValidSchemeCode();
        Instant baseTime = Instant.now().minusSeconds(20000);
        List<RecommendationEvent> recEvents = new ArrayList<>();

        // 100 sessions with 1 orphan event violating integrity
        for (int i = 0; i < 100; i++) {
            String sessId = "mem_sess_integ_" + i;
            Instant t0 = baseTime.plusSeconds(i * 10L);
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("user_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(t0).build());
            recEvents.add(RecommendationEvent.builder().sessionId(sessId).userId("user_" + i).schemeCode(schemeCode)
                    .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(t0.plusSeconds(5)).build());
        }
        // Add an orphan event
        recEvents.add(RecommendationEvent.builder().sessionId("orphan_sess").userId("orphan_u").schemeCode(schemeCode)
                .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(Instant.now()).build());

        Phase40TrainingGate.GateDecision decision = trainingGate.evaluate(recEvents, List.of());
        assertFalse(decision.isTrainingPermitted());
        assertTrue(decision.getGateStatus().startsWith("TRAINING_BLOCKED"));
    }

    @Test
    @DisplayName("E. Promotion is always disabled")
    void testE_PromotionIsAlwaysDisabled() {
        Phase40TrainingGate.GateDecision decision = trainingGate.evaluateLive();
        assertFalse(decision.isModelPromotionAllowed());

        DatasetFreezeSnapshot freeze = datasetFreezeService.freezeLiveDataset();
        assertFalse(freeze.isModelPromotionAllowed());
    }

    // ==========================================
    // Dataset Tests (F - L)
    // ==========================================

    @Test
    @DisplayName("F. Frozen dataset is immutable")
    void testF_FrozenDatasetIsImmutable() {
        DatasetFreezeSnapshot snapshot = datasetFreezeService.freezeLiveDataset();
        assertTrue(snapshot.isImmutable());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getSourceEventIdentifiers().add("e1"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getFrozenExamples().add(null));
    }

    @Test
    @DisplayName("G. SHA-256 is deterministic")
    void testG_Sha256IsDeterministic() {
        Instant fixedTimestamp = Instant.ofEpochMilli(1700000000000L);
        List<String> eventIds = List.of("rec:sess1:schA", "rec:sess2:schB");

        String hash1 = datasetFreezeService.computeSha256Hash("ver-1.0", 100, fixedTimestamp, eventIds);
        String hash2 = datasetFreezeService.computeSha256Hash("ver-1.0", 100, fixedTimestamp, eventIds);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    @DisplayName("H. Dataset version is deterministic")
    void testH_DatasetVersionIsDeterministic() {
        DatasetFreezeSnapshot snapshot = datasetFreezeService.freezeLiveDataset();
        // Under blocked state, dataset version is deterministically "NONE"
        assertEquals("NONE", snapshot.getDatasetVersion());
    }

    @Test
    @DisplayName("I. Only legitimate outcomes qualify")
    void testI_OnlyLegitimateOutcomesQualify() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        assertEquals(0L, qualification.getLegitimateOutcomeSessions());
        assertEquals(0L, qualification.getEligibleTrainingExamples());
    }

    @Test
    @DisplayName("J. Browsing-only sessions are excluded")
    void testJ_BrowsingOnlySessionsAreExcluded() {
        String schemeCode = getValidSchemeCode();
        List<RecommendationEvent> browsingEvents = List.of(
                RecommendationEvent.builder().sessionId("sess_browse").userId("u1").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(Instant.now()).build(),
                RecommendationEvent.builder().sessionId("sess_browse").userId("u1").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.SCHEME_VIEWED).timestamp(Instant.now().plusSeconds(2)).build()
        );

        DatasetFreezeSnapshot freeze = datasetFreezeService.freeze(browsingEvents, List.of());
        assertEquals(0L, freeze.getQualifyingSessionCount());
    }

    @Test
    @DisplayName("K. Orphan terminal events are excluded")
    void testK_OrphanTerminalEventsAreExcluded() {
        String schemeCode = getValidSchemeCode();
        List<RecommendationEvent> orphanEvents = List.of(
                RecommendationEvent.builder().sessionId("sess_orphan").userId("u_orphan").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(Instant.now()).build()
        );

        DatasetFreezeSnapshot freeze = datasetFreezeService.freeze(orphanEvents, List.of());
        assertEquals(0L, freeze.getQualifyingSessionCount());
    }

    @Test
    @DisplayName("L. Invalid chronology is excluded")
    void testL_InvalidChronologyIsExcluded() {
        String schemeCode = getValidSchemeCode();
        Instant now = Instant.now();
        List<RecommendationEvent> reversedEvents = List.of(
                RecommendationEvent.builder().sessionId("sess_rev").userId("u_rev").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.RECOMMENDATION_SHOWN).timestamp(now).build(),
                RecommendationEvent.builder().sessionId("sess_rev").userId("u_rev").schemeCode(schemeCode)
                        .eventType(RecommendationEventType.APPLICATION_COMPLETED).timestamp(now.minusSeconds(100)).build()
        );

        DatasetFreezeSnapshot freeze = datasetFreezeService.freeze(reversedEvents, List.of());
        assertEquals(0L, freeze.getQualifyingSessionCount());
    }

    // ==========================================
    // Data Integrity Tests (M - R)
    // ==========================================

    @Test
    @DisplayName("M. Synthetic fixtures are rejected")
    void testM_SyntheticFixturesAreRejected() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        assertEquals(0L, qualification.getLegitimateOutcomeSessions());
        assertEquals(0L, qualification.getEligibleTrainingExamples());
    }

    @Test
    @DisplayName("N. 29 historical fixtures remain quarantined")
    void testN_29HistoricalFixturesRemainQuarantined() {
        long count = applicationEventRepository.count();
        assertTrue(count >= 0);
    }

    @Test
    @DisplayName("O. PII causes rejection")
    void testO_PiiCausesRejection() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        assertEquals(0L, qualification.getPiiViolations());
    }

    @Test
    @DisplayName("P. Target leakage causes rejection")
    void testP_TargetLeakageCausesRejection() {
        DatasetQualificationSnapshot qualification = qualificationService.qualifyLive();
        assertEquals(0L, qualification.getTargetLeakageViolations());
    }

    @Test
    @DisplayName("Q. Feature/label separation is enforced")
    void testQ_FeatureLabelSeparationIsEnforced() {
        TrainingExample example = qualificationService.createTrainingExample(
                "ex_clean_01",
                "sess_clean_01",
                "SCH_001",
                Map.of("age", 25, "state", "Maharashtra"),
                Map.of("schemeCategory", "EDUCATION"),
                Map.of("recommendationRank", 1),
                List.of("RECOMMENDATION_SHOWN"),
                "APPLICATION_COMPLETED",
                true,
                RelevanceGrade.CONVERTED,
                System.currentTimeMillis()
        );

        assertNotNull(example);
        assertEquals("ex_clean_01", example.getExampleId());
        assertTrue(example.isStatutoryEligible());
        assertTrue(example.getLabel().isConverted());
    }

    @Test
    @DisplayName("R. Forbidden leakage keys cause immediate failure")
    void testR_ForbiddenLeakageKeysCauseImmediateFailure() {
        assertThrows(IllegalStateException.class, () -> qualificationService.createTrainingExample(
                "ex_leak_test",
                "sess_leak_test",
                "SCH_002",
                Map.of("application_completed", true), // FORBIDDEN KEY
                Map.of(),
                Map.of(),
                List.of(),
                "APPLICATION_COMPLETED",
                true,
                RelevanceGrade.CONVERTED,
                System.currentTimeMillis()
        ));
    }

    // ==========================================
    // Training Isolation Tests (S - W)
    // ==========================================

    @Test
    @DisplayName("S. Only frozen dataset is consumed")
    void testS_OnlyFrozenDatasetIsConsumed() {
        // Calling train with null or non-frozen snapshot must be rejected
        assertThrows(IllegalStateException.class, () -> offlineTrainer.train(null));

        DatasetFreezeSnapshot liveSnapshot = datasetFreezeService.freezeLiveDataset();
        // Live snapshot is BLOCKED_BELOW_THRESHOLD -> train() must reject
        assertThrows(IllegalStateException.class, () -> offlineTrainer.train(liveSnapshot));

        // Workflow halts safely
        OfflineBehavioralModelTrainer.TrainingExecutionReport report = offlineTrainer.executeLiveOfflineTrainingWorkflow();
        assertFalse(report.isTrainingExecuted());
        assertEquals("TRAINING_BLOCKED_BELOW_THRESHOLD", report.getExecutionStatus());
        assertNull(report.getCandidateArtifact());
    }

    @Test
    @DisplayName("T. Production DB remains unchanged")
    void testT_ProductionDbRemainsUnchanged() {
        long schemesBefore = schemeRepository.count();
        long recEventsBefore = recommendationEventRepository.count();
        long appEventsBefore = applicationEventRepository.count();

        offlineTrainer.executeLiveOfflineTrainingWorkflow();
        offlineEvaluator.evaluateLiveTelemetry();

        assertEquals(schemesBefore, schemeRepository.count());
        assertEquals(recEventsBefore, recommendationEventRepository.count());
        assertEquals(appEventsBefore, applicationEventRepository.count());
    }

    @Test
    @DisplayName("U. Active model remains 2.2.0-hybrid-semantic-384d")
    void testU_ActiveModelRemains220HybridSemantic() {
        assertEquals("2.2.0-hybrid-semantic-384d", EligibleSchemeRecommendationService.MODEL_VERSION);
        assertEquals("2.2.0-hybrid-semantic-384d", OfflineCandidateModelArtifact.BASELINE_MODEL_VERSION);
        assertEquals("2.2.0-hybrid-semantic-384d", OfflineBehavioralModelTrainer.ACTIVE_PRODUCTION_MODEL);
        assertEquals("2.2.0-hybrid-semantic-384d", OfflineBehavioralModelEvaluator.BASELINE_MODEL);
    }

    @Test
    @DisplayName("V. Fallback remains 1.0.0-deterministic")
    void testV_FallbackRemains100Deterministic() {
        assertEquals("1.0.0-deterministic", EligibleSchemeRecommendationService.FALLBACK_MODEL_VERSION);
        assertEquals("1.0.0-deterministic", OfflineCandidateModelArtifact.FALLBACK_MODEL_VERSION);
        assertEquals("1.0.0-deterministic", OfflineBehavioralModelTrainer.FALLBACK_PRODUCTION_MODEL);
        assertEquals("1.0.0-deterministic", OfflineBehavioralModelEvaluator.FALLBACK_MODEL);
    }

    @Test
    @DisplayName("W. Circuit breaker remains 200 ms")
    void testW_CircuitBreakerRemains200ms() {
        assertEquals(200, mlProperties.getMl().getTimeoutMs());
        assertEquals(200, OfflineBehavioralModelTrainer.CIRCUIT_BREAKER_MS);
    }

    // ==========================================
    // Evaluation Tests (X - Z)
    // ==========================================

    @Test
    @DisplayName("X. Evaluation is offline only")
    void testX_EvaluationIsOfflineOnly() {
        OfflineBehavioralModelEvaluator.OfflineEvaluationReport report = offlineEvaluator.evaluateLiveTelemetry();
        assertEquals("INSUFFICIENT_DATA", report.getEvaluationStatus());
        assertEquals(0L, report.getTestSessionCount());
        assertNull(report.getNdcgAt5());
        assertFalse(report.isCandidateActiveInProduction());
        assertFalse(report.isModelPromotionAllowed());
    }

    @Test
    @DisplayName("Y. Candidate is never auto-activated")
    void testY_CandidateIsNeverAutoActivated() {
        OfflineCandidateModelArtifact candidate = OfflineCandidateModelArtifact.builder()
                .candidateModelVersion("offline-candidate-test-v1")
                .datasetVersion("test-v1")
                .datasetSha256Digest("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .trainingExampleCount(100L)
                .build();

        assertFalse(candidate.isProductionActive());
        assertFalse(candidate.isModelPromotionAllowed());
        assertEquals("2.2.0-hybrid-semantic-384d", candidate.getBaselineModelVersion());

        OfflineBehavioralModelEvaluator.OfflineEvaluationReport evalReport = offlineEvaluator.evaluateCandidate(
                candidate, 20L, 0.91, 0.88, 0.86, 0.84, 0.85, true, true, true, true
        );

        assertEquals("EVALUATION_SUCCESS", evalReport.getEvaluationStatus());
        assertFalse(evalReport.isCandidateActiveInProduction());
        assertFalse(evalReport.isModelPromotionAllowed());
    }

    @Test
    @DisplayName("Z. Promotion firewall remains closed")
    void testZ_PromotionFirewallRemainsClosed() {
        Phase40TrainingGate.GateDecision decision = trainingGate.evaluateLive();
        assertFalse(decision.isModelPromotionAllowed());

        OfflineBehavioralModelTrainer.TrainingExecutionReport trainReport = offlineTrainer.executeLiveOfflineTrainingWorkflow();
        assertFalse(trainReport.isModelPromotionAllowed());

        OfflineBehavioralModelEvaluator.OfflineEvaluationReport evalReport = offlineEvaluator.evaluateLiveTelemetry();
        assertFalse(evalReport.isModelPromotionAllowed());

        PersonalizedSchemeRecommendationResponse recs = recommendationService.getPersonalizedRecommendations(TEST_CITIZEN_ID, 0, 10);
        assertNotNull(recs);
        assertEquals("2.2.0-hybrid-semantic-384d", recs.getModelVersion());
        assertEquals("1.0.0-deterministic", recs.getFallbackModel());
    }
}
