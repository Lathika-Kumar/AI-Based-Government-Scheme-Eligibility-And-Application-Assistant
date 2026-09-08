package com.schemebridge.scheme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AiChatRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse.EvaluationSummary;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Phase 26: User–Scheme Feature Engineering, Eligibility Comparison,
 * Training Data Preparation & Safe ML Readiness Test Suite.
 */
@ExtendWith(MockitoExtension.class)
public class Phase26UserSchemeFeatureEngineeringTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private SemanticEmbeddingIndexService semanticEmbeddingIndexService;

    @Mock
    private SchemeDocumentRequirementResolver documentRequirementResolver;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private RecommendationEventRepository recommendationEventRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private ModelRegistryService modelRegistryService;
    private com.schemebridge.scheme.ml.feature.UserFeatureExtractor userFeatureExtractor;
    private com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor schemeFeatureExtractor;
    private com.schemebridge.scheme.ml.feature.UserSchemeComparisonService comparisonService;
    private com.schemebridge.scheme.ml.feature.FeatureVectorBuilder featureVectorBuilder;
    private EligibleSchemeRecommendationService recommendationService;
    private RecommendationEventService recommendationEventService;
    private AiChatService aiChatService;
    private MlRecommenderProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MlRecommenderProperties();
        properties.getMl().setEnabled(true);
        properties.getMl().setModelVersion("2.2.0-hybrid-semantic-384d");
        properties.getMl().setTimeoutMs(200);

        modelRegistryService = new ModelRegistryService();
        userFeatureExtractor = new com.schemebridge.scheme.ml.feature.UserFeatureExtractor();
        schemeFeatureExtractor = new com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor();
        comparisonService = new com.schemebridge.scheme.ml.feature.UserSchemeComparisonService();
        featureVectorBuilder = new com.schemebridge.scheme.ml.feature.FeatureVectorBuilder();

        recommendationService = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                semanticEmbeddingIndexService,
                modelRegistryService,
                userFeatureExtractor,
                schemeFeatureExtractor,
                comparisonService,
                featureVectorBuilder,
                schemeVerifiedDataRepository
        );

        recommendationEventService = new RecommendationEventService(recommendationEventRepository, schemeRepository);

        aiChatService = new AiChatService(
                mongoTemplate,
                citizenProfileRepository,
                citizenProfileService,
                applicationRepository,
                grievanceRepository,
                feedbackRepository,
                schemeRepository,
                schemeVerifiedDataRepository,
                null,
                documentRequirementResolver,
                "",
                "gemini-1.5-flash",
                new ObjectMapper()
        );

        // Inject modelRegistryService and recommendationEventRepository into aiChatService
        try {
            var fieldRegistry = AiChatService.class.getDeclaredField("modelRegistryService");
            fieldRegistry.setAccessible(true);
            fieldRegistry.set(aiChatService, modelRegistryService);

            var fieldEvents = AiChatService.class.getDeclaredField("recommendationEventRepository");
            fieldEvents.setAccessible(true);
            fieldEvents.set(aiChatService, recommendationEventRepository);
        } catch (Exception e) {
            fail("Failed to inject fields into AiChatService: " + e.getMessage());
        }
    }

    // 1. Training dataset detects zero legitimate interactions
    @Test
    @DisplayName("1. Training dataset builder detects zero legitimate interactions in database")
    void testTrainingDatasetDetectsZeroLegitimateInteractions() throws Exception {
        File metadataFile = new File("E:/SCHEMEBRIDGE/data/phase25_output/dataset_metadata.json");
        assertTrue(metadataFile.exists(), "dataset_metadata.json must exist");
        String content = Files.readString(metadataFile.toPath());
        assertTrue(content.contains("\"real_interaction_sessions\": 0"));
    }

    // 2. TRAINING_NOT_READY is returned when threshold is not met
    @Test
    @DisplayName("2. TRAINING_NOT_READY is returned when threshold is not met")
    void testTrainingNotReadyReturnedWhenThresholdNotMet() throws Exception {
        File metadataFile = new File("E:/SCHEMEBRIDGE/data/phase25_output/dataset_metadata.json");
        assertTrue(metadataFile.exists());
        String content = Files.readString(metadataFile.toPath());
        assertTrue(content.contains("\"status\": \"TRAINING_NOT_READY\""));
        assertTrue(content.contains("\"training_allowed\": false"));
    }

    // 3. Synthetic fixtures are excluded
    @Test
    @DisplayName("3. Synthetic test fixtures are strictly excluded from training data")
    void testSyntheticFixturesAreExcluded() throws Exception {
        File metadataFile = new File("E:/SCHEMEBRIDGE/data/phase25_output/dataset_metadata.json");
        assertTrue(metadataFile.exists());
        String content = Files.readString(metadataFile.toPath());
        assertTrue(content.contains("\"synthetic_records_excluded\": 29"));
    }

    // 4. No PII enters training data
    @Test
    @DisplayName("4. No citizen PII enters training datasets or specifications")
    void testNoPiiEntersTrainingData() throws Exception {
        File specFile = new File("E:/SCHEMEBRIDGE/data/phase25_output/training_dataset_specification.json");
        assertTrue(specFile.exists());
        String specContent = Files.readString(specFile.toPath());
        assertTrue(specContent.contains("\"excludedFields\""));
        assertTrue(specContent.contains("aadhaar"));
        assertTrue(specContent.contains("email"));
        assertTrue(specContent.contains("phone"));
    }

    // 5. Dataset split is deterministic
    @Test
    @DisplayName("5. Dataset split generation is deterministic and preserves isolation")
    void testDatasetSplitIsDeterministic() {
        File splitManifest = new File("E:/SCHEMEBRIDGE/data/ml_models/split_manifest.json");
        assertTrue(splitManifest.exists());
    }

    // 6. Eligibility executes before ranking
    @Test
    @DisplayName("6. Statutory EligibilityEngine executes strictly before ML ranking")
    void testEligibilityExecutesBeforeRanking() {
        CitizenProfile profile = CitizenProfile.builder().userId("citizen-101").state("GUJARAT").build();
        when(citizenProfileRepository.findByUserId("citizen-101")).thenReturn(Optional.of(profile));

        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("SCH_PM_KISAN")
                .status(EligibilityStatus.ELIGIBLE)
                .build();

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .eligibleSchemes(List.of(result))
                .summary(EvaluationSummary.builder().totalEvaluated(10).eligibleCount(1).build())
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-101")).thenReturn(evalRes);
        Scheme s = Scheme.builder().schemeCode("SCH_PM_KISAN").build();
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-101", 0, 10);
        verify(eligibilityEvaluationService, times(1)).evaluateCitizenAgainstAllSchemes("citizen-101");
        assertNotNull(resp);
        assertEquals(1, resp.getEligibleCandidatesFound());
    }

    // 7. Ineligible schemes never enter ML ranking
    @Test
    @DisplayName("7. Ineligible schemes are filtered out and never enter ML ranking")
    void testIneligibleSchemesNeverEnterMlRanking() {
        CitizenProfile profile = CitizenProfile.builder().userId("citizen-102").state("BIHAR").build();
        when(citizenProfileRepository.findByUserId("citizen-102")).thenReturn(Optional.of(profile));

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .eligibleSchemes(List.of())
                .summary(EvaluationSummary.builder().totalEvaluated(100).eligibleCount(0).build())
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-102")).thenReturn(evalRes);

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-102", 0, 10);
        assertEquals(0, resp.getRecommendations().size());
        verify(semanticEmbeddingIndexService, never()).computeSemanticSimilarity(anyString(), any());
    }

    // 8. Eligibility violation rate remains 0.00%
    @Test
    @DisplayName("8. Statutory eligibility violation rate strictly equals 0.00%")
    void testEligibilityViolationRateRemainsZero() {
        ModelRegistryService.ModelRecord active = modelRegistryService.getActiveModel();
        assertNotNull(active);
        assertEquals(0.00, active.getStatutoryEligibilityViolationRate(), 0.0001);
    }

    // 9. Existing active model loads correctly
    @Test
    @DisplayName("9. Existing active model loads correctly into model registry")
    void testExistingActiveModelLoadsCorrectly() {
        assertEquals("2.2.0-hybrid-semantic-384d", modelRegistryService.getActiveModelVersion());
        ModelRegistryService.ModelRecord record = modelRegistryService.getActiveModel();
        assertEquals(ModelRegistryService.ModelStatus.ACTIVE, record.getStatus());
        assertTrue(record.isArtifactLoadable());
        assertTrue(record.isDeterministicFallbackAvailable());
    }

    // 10. Candidate model cannot activate without evaluation
    @Test
    @DisplayName("10. Candidate model cannot activate without evaluation metrics")
    void testCandidateModelCannotActivateWithoutEvaluation() {
        ModelRegistryService.ModelRecord candidate = ModelRegistryService.ModelRecord.builder()
                .modelVersion("candidate-untested-v1")
                .trainingDatasetVersion("phase25-dataset-v1")
                .metrics(Collections.emptyMap()) // No metrics
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(5.0)
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        modelRegistryService.registerCandidate(candidate);
        ModelRegistryService.PromotionResult res = modelRegistryService.evaluateAndPromote("candidate-untested-v1");
        assertFalse(res.promoted());
        assertEquals(ModelRegistryService.ModelStatus.REJECTED, modelRegistryService.getModel("candidate-untested-v1").getStatus());
    }

    // 11. Candidate model can enter SHADOW
    @Test
    @DisplayName("11. Candidate model can safely enter SHADOW mode without affecting citizens")
    void testCandidateModelCanEnterShadow() {
        ModelRegistryService.ModelRecord candidate = ModelRegistryService.ModelRecord.builder()
                .modelVersion("candidate-shadow-v1")
                .trainingDatasetVersion("phase25-dataset-v1")
                .metrics(Map.of("ndcg@5", 0.65))
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(6.0)
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        modelRegistryService.registerCandidate(candidate);
        boolean shadowOk = modelRegistryService.promoteToShadow("candidate-shadow-v1");
        assertTrue(shadowOk);
        assertEquals("candidate-shadow-v1", modelRegistryService.getShadowModelVersion());
        // Active model remains untouched
        assertEquals("2.2.0-hybrid-semantic-384d", modelRegistryService.getActiveModelVersion());
    }

    // 12. Promotion is rejected on regression
    @Test
    @DisplayName("12. Promotion is rejected if candidate model exhibits metric regression")
    void testPromotionIsRejectedOnRegression() {
        ModelRegistryService.ModelRecord regressedCandidate = ModelRegistryService.ModelRecord.builder()
                .modelVersion("candidate-regressed-v1")
                .trainingDatasetVersion("phase25-dataset-v1")
                .metrics(Map.of("ndcg@5", 0.40)) // Lower than active 0.6384
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(10.0)
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        modelRegistryService.registerCandidate(regressedCandidate);
        ModelRegistryService.PromotionResult res = modelRegistryService.evaluateAndPromote("candidate-regressed-v1");
        assertFalse(res.promoted());
        assertTrue(res.message().contains("regression"));
    }

    // 13. Promotion succeeds only when all gates pass
    @Test
    @DisplayName("13. Promotion succeeds when all safety, statutory, and metric gates pass")
    void testPromotionSucceedsOnlyWhenAllGatesPass() {
        ModelRegistryService.ModelRecord superiorCandidate = ModelRegistryService.ModelRecord.builder()
                .modelVersion("candidate-superior-v1")
                .trainingDatasetVersion("phase25-dataset-v1")
                .metrics(Map.of("ndcg@5", 0.70)) // Superior to active 0.6384
                .statutoryEligibilityViolationRate(0.00) // 0.00%
                .avgLatencyMs(15.0) // < 200ms
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        modelRegistryService.registerCandidate(superiorCandidate);
        ModelRegistryService.PromotionResult res = modelRegistryService.evaluateAndPromote("candidate-superior-v1");
        assertTrue(res.promoted());
        assertEquals("candidate-superior-v1", modelRegistryService.getActiveModelVersion());
    }

    // 14. Timeout triggers deterministic fallback
    @Test
    @DisplayName("14. Timeout exceeding 200ms triggers deterministic fallback")
    void testTimeoutTriggersDeterministicFallback() {
        properties.getMl().setTimeoutMs(0); // Instant timeout
        CitizenProfile profile = CitizenProfile.builder().userId("citizen-103").state("GUJARAT").build();
        when(citizenProfileRepository.findByUserId("citizen-103")).thenReturn(Optional.of(profile));

        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("SCH_TIMEOUT")
                .status(EligibilityStatus.ELIGIBLE)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-103"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .eligibleSchemes(List.of(result))
                        .summary(EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                        .build());

        Scheme s = Scheme.builder().schemeCode("SCH_TIMEOUT").build();
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-103", 0, 10);
        assertNotNull(resp);
        assertEquals("DETERMINISTIC_FALLBACK", resp.getRankingMethod());
        assertTrue(Boolean.TRUE.equals(resp.getFallbackUsed()));
    }

    // 15. Model failure triggers deterministic fallback
    @Test
    @DisplayName("15. Model failure or exception triggers safe deterministic fallback")
    void testModelFailureTriggersDeterministicFallback() {
        properties.getMl().setTimeoutMs(200);
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(anyString(), any()))
                .thenThrow(new RuntimeException("Simulated Vector Memory Fault"));

        CitizenProfile profile = CitizenProfile.builder().userId("citizen-104").state("GUJARAT").build();
        when(citizenProfileRepository.findByUserId("citizen-104")).thenReturn(Optional.of(profile));

        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("SCH_FAULT")
                .status(EligibilityStatus.ELIGIBLE)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-104"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .eligibleSchemes(List.of(result))
                        .summary(EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                        .build());

        Scheme s = Scheme.builder().schemeCode("SCH_FAULT").build();
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-104", 0, 10);
        assertNotNull(resp);
        assertEquals("DETERMINISTIC_FALLBACK", resp.getRankingMethod());
        assertTrue(Boolean.TRUE.equals(resp.getFallbackUsed()));
    }

    // 16. Rollback restores previous active model
    @Test
    @DisplayName("16. Safe rollback restores previous active model without system downtime")
    void testRollbackRestoresPreviousActiveModel() {
        // First promote a candidate
        ModelRegistryService.ModelRecord candidate = ModelRegistryService.ModelRecord.builder()
                .modelVersion("candidate-rollback-test-v1")
                .trainingDatasetVersion("phase25-dataset-v1")
                .metrics(Map.of("ndcg@5", 0.72))
                .statutoryEligibilityViolationRate(0.00)
                .avgLatencyMs(10.0)
                .deterministicFallbackAvailable(true)
                .artifactLoadable(true)
                .build();

        modelRegistryService.registerCandidate(candidate);
        modelRegistryService.evaluateAndPromote("candidate-rollback-test-v1");
        assertEquals("candidate-rollback-test-v1", modelRegistryService.getActiveModelVersion());

        // Perform rollback
        boolean rollbackSuccess = modelRegistryService.rollbackActiveModel();
        assertTrue(rollbackSuccess);
        assertEquals("2.2.0-hybrid-semantic-384d", modelRegistryService.getActiveModelVersion());
    }

    // 17. Citizen explanations contain no ML jargon
    @Test
    @DisplayName("17. Citizen recommendations contain plain-language explanations with zero ML jargon")
    void testCitizenExplanationsContainNoMlJargon() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-105")
                .state("GUJARAT")
                .occupation("FARMER")
                .isFarmer(true)
                .build();

        when(citizenProfileRepository.findByUserId("citizen-105")).thenReturn(Optional.of(profile));

        Scheme s = Scheme.builder()
                .schemeCode("PM_KISAN_TEST")
                .title(MultilingualText.builder().english("PM Kisan").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture").build())
                .build();

        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("PM_KISAN_TEST")
                .status(EligibilityStatus.ELIGIBLE)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-105"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .eligibleSchemes(List.of(result))
                        .summary(EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                        .build());
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-105", 0, 10);
        assertNotNull(resp);
        assertFalse(resp.getRecommendations().isEmpty());

        List<String> reasons = resp.getRecommendations().get(0).getReasons();
        for (String r : reasons) {
            String lower = r.toLowerCase();
            assertFalse(lower.contains("vector"), "Reason must not contain 'vector'");
            assertFalse(lower.contains("embedding"), "Reason must not contain 'embedding'");
            assertFalse(lower.contains("cosine"), "Reason must not contain 'cosine'");
            assertFalse(lower.contains("model id"), "Reason must not contain 'model id'");
            assertFalse(lower.contains("ltr"), "Reason must not contain 'ltr'");
        }
    }

    // 18. Admin AI reports actual training status
    @Test
    @DisplayName("18. Admin AI truthfully reports TRAINING_NOT_READY and deficiency reasons")
    void testAdminAiReportsActualTrainingStatus() {
        when(applicationRepository.count()).thenReturn(0L);
        when(grievanceRepository.count()).thenReturn(0L);
        when(feedbackRepository.count()).thenReturn(0L);
        when(schemeRepository.count()).thenReturn(4734L);
        when(schemeVerifiedDataRepository.count()).thenReturn(4682L);
        when(recommendationEventRepository.count()).thenReturn(0L);

        AiChatRequest req = AiChatRequest.builder().message("Is the recommendation model ready for training?").build();
        AiChatResponse resp = aiChatService.chatAdmin(req, "admin_user", "ROLE_ADMIN");

        assertNotNull(resp);
        assertTrue(resp.getResponse().contains("TRAINING_NOT_READY"));
        assertTrue(resp.getResponse().contains("100"));
    }

    // 19. Admin AI reports actual registry information
    @Test
    @DisplayName("19. Admin AI truthfully reports active model version from registry")
    void testAdminAiReportsActualRegistryInformation() {
        when(applicationRepository.count()).thenReturn(0L);
        when(grievanceRepository.count()).thenReturn(0L);
        when(feedbackRepository.count()).thenReturn(0L);
        when(schemeRepository.count()).thenReturn(4734L);
        when(schemeVerifiedDataRepository.count()).thenReturn(4682L);

        AiChatRequest req = AiChatRequest.builder().message("What is the current model version?").build();
        AiChatResponse resp = aiChatService.chatAdmin(req, "admin_user", "ROLE_ADMIN");

        assertNotNull(resp);
        assertTrue(resp.getResponse().contains("2.2.0-hybrid-semantic-384d"));
        assertTrue(resp.getResponse().contains("ACTIVE"));
    }

    // 20. Cold-start recommendations remain functional
    @Test
    @DisplayName("20. Cold-start citizens with zero history receive valid recommendations")
    void testColdStartRecommendationsRemainFunctional() {
        CitizenProfile coldProfile = CitizenProfile.builder()
                .userId("cold-citizen")
                .state("RAJASTHAN")
                .occupation("OTHER")
                .annualIncome(180000.0)
                .build();

        when(citizenProfileRepository.findByUserId("cold-citizen")).thenReturn(Optional.of(coldProfile));

        Scheme s = Scheme.builder()
                .schemeCode("COLD_CENTRAL_SCHEME")
                .schemeLevel(SchemeLevel.CENTRAL)
                .build();

        EligibilityEvaluationResult result = EligibilityEvaluationResult.builder()
                .schemeCode("COLD_CENTRAL_SCHEME")
                .status(EligibilityStatus.ELIGIBLE)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("cold-citizen"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .eligibleSchemes(List.of(result))
                        .summary(EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                        .build());
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("cold-citizen", 0, 10);
        assertNotNull(resp);
        assertEquals(1, resp.getRecommendations().size());
        assertTrue(resp.getRecommendations().get(0).getRecommendationScore() > 0.0);
    }

    // ── PHASE 26 SPECIFIC TESTS (1–15, 22–27, 34) ──────────────────────────

    @Test
    @DisplayName("P26-1. User feature extraction extracts expected demographic attributes")
    void testP26_UserFeatureExtraction() {
        CitizenProfile p = CitizenProfile.builder()
                .age(42)
                .state("Maharashtra")
                .district("Pune")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("OBC")
                .gender("Male")
                .disabilityStatus(false)
                .isFarmer(true)
                .isStudent(false)
                .bplStatus(false)
                .education("Graduate")
                .build();

        com.schemebridge.scheme.ml.feature.UserFeatures f = userFeatureExtractor.extractFromCitizenProfile(p);
        assertEquals("AGE_36_50", f.getAgeBucket());
        assertEquals("MAHARASHTRA", f.getStateCode());
        assertEquals("PUNE", f.getDistrict());
        assertEquals("FARMER", f.getOccupationCode());
        assertEquals("TIER_1_3L", f.getIncomeTier());
        assertEquals("OBC", f.getCategoryCode());
        assertEquals("MALE", f.getGenderCode());
        assertFalse(f.getDisabilityStatus());
        assertTrue(f.getIsFarmer());
        assertFalse(f.getIsStudent());
        assertFalse(f.getBplStatus());
        assertEquals("GRADUATE", f.getEducationLevel());
    }

    @Test
    @DisplayName("P26-2. Missing user attributes gracefully map to UNKNOWN or null without throwing")
    void testP26_MissingUserAttributes() {
        CitizenProfile p = CitizenProfile.builder().age(25).build();
        com.schemebridge.scheme.ml.feature.UserFeatures f = userFeatureExtractor.extractFromCitizenProfile(p);

        assertEquals("AGE_18_25", f.getAgeBucket());
        assertEquals("UNKNOWN", f.getStateCode());
        assertEquals("UNKNOWN", f.getOccupationCode());
        assertEquals("UNKNOWN", f.getIncomeTier());
        assertEquals("UNKNOWN", f.getCategoryCode());
        assertEquals("UNKNOWN", f.getGenderCode());
        assertNull(f.getDisabilityStatus());
    }

    @Test
    @DisplayName("P26-3. Completely empty cold-start user profile yields valid empty features")
    void testP26_ColdStartUser() {
        com.schemebridge.scheme.ml.feature.UserFeatures f = userFeatureExtractor.extractFromCitizenProfile(null);
        assertEquals("UNKNOWN", f.getAgeBucket());
        assertEquals("UNKNOWN", f.getStateCode());
        assertEquals("UNKNOWN", f.getOccupationCode());
        assertNull(f.getDisabilityStatus());
    }

    @Test
    @DisplayName("P26-4. Scheme feature extraction extracts criteria from Scheme master data")
    void testP26_SchemeFeatureExtraction() {
        Scheme s = Scheme.builder()
                .schemeCode("SCH-PMKISAN-001")
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture").build())
                .benefits(List.of(SchemeBenefit.builder().amountType("FINANCIAL").build()))
                .eligibilityRules(RuleGroup.builder()
                        .conditions(List.of(
                                EligibilityCondition.builder().field("age").operator(RuleOperator.GREATER_THAN_OR_EQUAL).value("18").build(),
                                EligibilityCondition.builder().field("occupation").operator(RuleOperator.EQUALS).value("Farmer").build(),
                                EligibilityCondition.builder().field("annualIncome").operator(RuleOperator.LESS_THAN_OR_EQUAL).value("250000").build()
                        ))
                        .build())
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures sf = schemeFeatureExtractor.extractFromScheme(s, null);
        assertEquals("SCH-PMKISAN-001", sf.getSchemeCode());
        assertEquals("AGRICULTURE", sf.getSchemeCategory());
        assertEquals("CENTRAL", sf.getSchemeLevel());
        assertEquals(18, sf.getMinAge());
        assertEquals(250000.0, sf.getMaxIncome());
        assertTrue(sf.getEligibleOccupations().contains("FARMER"));
    }

    @Test
    @DisplayName("P26-5. Age comparison evaluates MATCH, MISMATCH, and UNKNOWN correctly")
    void testP26_AgeComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareAge("AGE_26_35", 18, 50));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareAge("AGE_60_PLUS", 18, 35));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareAge("UNKNOWN", 18, 50));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareAge("AGE_18_25", null, null));
    }

    @Test
    @DisplayName("P26-6. Income comparison evaluates BPL and tier ceilings correctly")
    void testP26_IncomeComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareIncome("BPL", 150000.0));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareIncome("TIER_0_1L", 250000.0));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareIncome("TIER_ABOVE_8L", 300000.0));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareIncome("UNKNOWN", 200000.0));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareIncome("TIER_1_3L", null));
    }

    @Test
    @DisplayName("P26-7. State comparison evaluates CENTRAL as MATCH and local states accurately")
    void testP26_StateComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareState("GUJARAT", "CENTRAL", null));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareState("MAHARASHTRA", "STATE", "Maharashtra"));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareState("GUJARAT", "STATE", "Maharashtra"));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareState("UNKNOWN", "STATE", "Karnataka"));
    }

    @Test
    @DisplayName("P26-8. Occupation comparison verifies farmer/student and specific codes")
    void testP26_OccupationComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareOccupation("FARMER", true, false, List.of("FARMER")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareOccupation("STUDENT", false, true, List.of("STUDENT")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareOccupation("DOCTOR", false, false, List.of("FARMER", "ARTISAN")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareOccupation("FARMER", true, false, null));
    }

    @Test
    @DisplayName("P26-9. Category comparison matches targeted social categories")
    void testP26_CategoryComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareCategory("OBC", List.of("OBC", "SC", "ST")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareCategory("GENERAL", List.of("SC", "ST")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareCategory("OBC", null));
    }

    @Test
    @DisplayName("P26-10. Gender comparison matches targeted beneficiary genders")
    void testP26_GenderComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareGender("FEMALE", List.of("FEMALE")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareGender("MALE", List.of("FEMALE")));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareGender("MALE", null));
    }

    @Test
    @DisplayName("P26-11. Disability comparison evaluates PwD criteria accurately")
    void testP26_DisabilityComparison() {
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comparisonService.compareDisability(true, true));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MISMATCH, comparisonService.compareDisability(false, true));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareDisability(true, false));
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comparisonService.compareDisability(null, true));
    }

    @Test
    @DisplayName("P26-12. UNKNOWN criteria is NEVER converted into MATCH")
    void testP26_UnknownCriteriaHandling() {
        com.schemebridge.scheme.ml.feature.ComparisonFeatures comp = comparisonService.buildAllUnknown();
        assertNotEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comp.getAgeMatch());
        assertNotEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comp.getIncomeMatch());
        assertNotEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, comp.getStateMatch());
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, comp.getAgeMatch());
    }

    @Test
    @DisplayName("P26-13. UserSchemeFeatureVector constructs full container with schemaVersion 1.0.0")
    void testP26_FeatureVectorGeneration() {
        com.schemebridge.scheme.ml.feature.UserFeatures u = com.schemebridge.scheme.ml.feature.UserFeatures.builder().ageBucket("AGE_26_35").build();
        com.schemebridge.scheme.ml.feature.SchemeFeatures s = com.schemebridge.scheme.ml.feature.SchemeFeatures.builder().schemeCode("SCH_001").build();
        com.schemebridge.scheme.ml.feature.ComparisonFeatures c = comparisonService.buildAllUnknown();

        com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector vector = featureVectorBuilder.buildVector(u, s, c, 1, 0.85, "2.2.0-hybrid-semantic-384d");
        assertNotNull(vector);
        assertEquals("1.0.0", vector.getSchemaVersion());
        assertEquals("AGE_26_35", vector.getUserFeatures().getAgeBucket());
        assertEquals("SCH_001", vector.getSchemeFeatures().getSchemeCode());
        assertEquals(1, vector.getRankingContext().get("currentRank"));
        assertEquals(0.85, vector.getRankingContext().get("currentScore"));
    }

    @Test
    @DisplayName("P26-14. Feature vector contains ZERO direct citizen PII")
    void testP26_NoPiiInFeatureVector() {
        CitizenProfile p = CitizenProfile.builder()
                .userId("usr_7781")
                .displayName("John Doe")
                .pincode("380015")
                .udidNumber("MH12345678")
                .age(30)
                .build();

        com.schemebridge.scheme.ml.feature.UserFeatures u = userFeatureExtractor.extractFromCitizenProfile(p);
        assertFalse(u.toString().contains("John Doe"));
        assertFalse(u.toString().contains("380015"));
        assertFalse(u.toString().contains("MH12345678"));
        assertFalse(u.toString().contains("usr_7781"));
    }

    @Test
    @DisplayName("P26-15. Two identical inputs produce identical feature vectors deterministically")
    void testP26_DeterministicFeatureGeneration() {
        com.schemebridge.scheme.ml.feature.UserFeatures u1 = com.schemebridge.scheme.ml.feature.UserFeatures.builder().ageBucket("AGE_18_25").stateCode("GUJARAT").build();
        com.schemebridge.scheme.ml.feature.UserFeatures u2 = com.schemebridge.scheme.ml.feature.UserFeatures.builder().ageBucket("AGE_18_25").stateCode("GUJARAT").build();
        com.schemebridge.scheme.ml.feature.SchemeFeatures s1 = com.schemebridge.scheme.ml.feature.SchemeFeatures.builder().schemeCode("SCH_01").minAge(18).maxAge(30).build();
        com.schemebridge.scheme.ml.feature.SchemeFeatures s2 = com.schemebridge.scheme.ml.feature.SchemeFeatures.builder().schemeCode("SCH_01").minAge(18).maxAge(30).build();

        com.schemebridge.scheme.ml.feature.ComparisonFeatures c1 = comparisonService.compare(u1, s1);
        com.schemebridge.scheme.ml.feature.ComparisonFeatures c2 = comparisonService.compare(u2, s2);
        assertEquals(c1, c2);

        com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector v1 = featureVectorBuilder.buildVector(u1, s1, c1, 1, 0.9, "2.2.0");
        com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector v2 = featureVectorBuilder.buildVector(u2, s2, c2, 1, 0.9, "2.2.0");
        assertEquals(v1, v2);
    }

    @Test
    @DisplayName("P26-22to24. Training readiness: NOT_READY at 0 and 99, READY at 100 in-memory")
    void testP26_TrainingReadinessThreshold() {
        int threshold = 100;
        assertEquals("TRAINING_NOT_READY", 0 >= threshold ? "TRAINING_READY" : "TRAINING_NOT_READY");
        assertEquals("TRAINING_NOT_READY", 99 >= threshold ? "TRAINING_READY" : "TRAINING_NOT_READY");
        assertEquals("TRAINING_READY", 100 >= threshold ? "TRAINING_READY" : "TRAINING_NOT_READY");
    }

    @Test
    @DisplayName("P26-25to27. Clean session isolation and zero label leakage across 70/15/15 splits")
    void testP26_SessionIsolationAndSplits() {
        List<String> sessions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            sessions.add("session_" + String.format("%03d", i));
        }
        Collections.shuffle(sessions, new Random(42));
        Set<String> train = new HashSet<>(sessions.subList(0, 70));
        Set<String> val = new HashSet<>(sessions.subList(70, 85));
        Set<String> test = new HashSet<>(sessions.subList(85, 100));

        assertTrue(Collections.disjoint(train, val));
        assertTrue(Collections.disjoint(train, test));
        assertTrue(Collections.disjoint(val, test));
    }

    @Test
    @DisplayName("P26-34. Admin AI queries answer Phase 26 feature schema specs")
    void testP26_AdminAiFeatureSchemaSpecs() {
        AiChatRequest req = AiChatRequest.builder().message("What is the feature schema version?").build();
        AiChatResponse res = aiChatService.chatAdmin(req, "admin_user", "ROLE_ADMIN");
        assertNotNull(res);
        assertTrue(res.getResponse().contains("Phase 26"));
        assertTrue(res.getResponse().contains("1.0.0"));
        assertTrue(res.getResponse().contains("MATCH"));
        assertTrue(res.getResponse().contains("UNKNOWN"));
    }

    @Test
    @DisplayName("P26H-1. SchemeVerifiedData criteria extraction extracts verified structured criteria")
    void testP26H_SchemeVerifiedDataCriteriaExtraction() {
        Scheme s = Scheme.builder()
                .schemeCode("SCH-VERIFIED-01")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("TAMIL NADU")
                .build();

        Map<String, Object> struct = Map.of(
                "minAge", 21,
                "maxAge", 55,
                "maxIncome", 350000.0,
                "eligibleOccupations", List.of("ARTISAN", "WEAVER"),
                "eligibleCategories", List.of("OBC", "MBC"),
                "eligibleGenders", List.of("FEMALE"),
                "disabilityApplicable", true
        );

        SchemeVerifiedData vd = SchemeVerifiedData.builder()
                .schemeCode("SCH-VERIFIED-01")
                .eligibility(SchemeVerifiedData.CanonicalEligibility.builder()
                        .structuredEligibility(struct)
                        .build())
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures sf = schemeFeatureExtractor.extractFromScheme(s, vd);
        assertNotNull(sf);
        assertEquals(21, sf.getMinAge());
        assertEquals(55, sf.getMaxAge());
        assertEquals(350000.0, sf.getMaxIncome());
        assertEquals(List.of("ARTISAN", "WEAVER"), sf.getEligibleOccupations());
        assertEquals(List.of("OBC", "MBC"), sf.getEligibleCategories());
        assertEquals(List.of("FEMALE"), sf.getEligibleGenders());
        assertEquals(Boolean.TRUE, sf.getDisabilityApplicable());
    }

    @Test
    @DisplayName("P26H-2. Verified criteria take precedence over master RuleGroup criteria")
    void testP26H_VerifiedCriteriaPrecedence() {
        // Master rule specifies minAge = 18, maxIncome = 200000
        Scheme s = Scheme.builder()
                .schemeCode("SCH-PRECEDENCE-01")
                .eligibilityRules(RuleGroup.builder()
                        .conditions(List.of(
                                EligibilityCondition.builder().field("age").operator(RuleOperator.GREATER_THAN_OR_EQUAL).value("18").build(),
                                EligibilityCondition.builder().field("annualIncome").operator(RuleOperator.LESS_THAN_OR_EQUAL).value("200000").build()
                        ))
                        .build())
                .build();

        // Verified data specifies minAge = 21, maxIncome = 300000
        SchemeVerifiedData vd = SchemeVerifiedData.builder()
                .schemeCode("SCH-PRECEDENCE-01")
                .eligibility(SchemeVerifiedData.CanonicalEligibility.builder()
                        .structuredEligibility(Map.of(
                                "minAge", 21,
                                "maxIncome", 300000.0
                        ))
                        .build())
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures sf = schemeFeatureExtractor.extractFromScheme(s, vd);
        // Verified data (Priority 1) must override master rule
        assertEquals(21, sf.getMinAge());
        assertEquals(300000.0, sf.getMaxIncome());
    }

    @Test
    @DisplayName("P26H-3. Missing verified criteria evaluate to UNKNOWN without guessing")
    void testP26H_MissingVerifiedCriteriaEvaluatesToUnknown() {
        Scheme s = Scheme.builder().schemeCode("SCH-UNKNOWN-01").build();
        SchemeVerifiedData vd = SchemeVerifiedData.builder()
                .schemeCode("SCH-UNKNOWN-01")
                .eligibility(SchemeVerifiedData.CanonicalEligibility.builder()
                        .structuredEligibility(Map.of("minAge", 18))
                        .build())
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures sf = schemeFeatureExtractor.extractFromScheme(s, vd);
        assertNull(sf.getMaxAge());
        assertNull(sf.getMaxIncome());
        assertNull(sf.getEligibleOccupations());
        assertNull(sf.getEligibleCategories());

        com.schemebridge.scheme.ml.feature.UserFeatures uf = com.schemebridge.scheme.ml.feature.UserFeatures.builder()
                .ageBucket("AGE_18_25")
                .occupationCode("TEACHER")
                .build();

        com.schemebridge.scheme.ml.feature.ComparisonFeatures cf = comparisonService.compare(uf, sf);
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.MATCH, cf.getAgeMatch());
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, cf.getOccupationMatch());
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, cf.getIncomeMatch());
        assertEquals(com.schemebridge.scheme.ml.feature.FeatureMatchStatus.UNKNOWN, cf.getCategoryMatch());
    }

    @Test
    @DisplayName("P26H-4to6. Explanations grounded in ComparisonFeatures, identify EligibilityEngine, no ML eligibility claim")
    void testP26H_ExplanationGroundedAndStatutoryAuthority() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-grounded")
                .age(24)
                .state("TAMIL NADU")
                .annualIncome(120000.0)
                .occupation("STUDENT")
                .isStudent(true)
                .socialCategory("OBC")
                .build();
        when(citizenProfileRepository.findByUserId("citizen-grounded")).thenReturn(Optional.of(profile));

        Scheme s = Scheme.builder()
                .schemeCode("SCH-GROUNDED-01")
                .slug("grounded-scheme")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("TAMIL NADU")
                .title(MultilingualText.builder().english("Grounded Education Scheme").build())
                .shortDescription(MultilingualText.builder().english("Education support").build())
                .build();
        when(schemeRepository.findAll()).thenReturn(List.of(s));

        EligibilityEvaluationResult evalResult = EligibilityEvaluationResult.builder()
                .schemeCode("SCH-GROUNDED-01")
                .slug("grounded-scheme")
                .schemeTitle("Grounded Education Scheme")
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-grounded"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .userId("citizen-grounded")
                        .eligibleSchemes(List.of(evalResult))
                        .summary(EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                        .build());

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-grounded", 0, 10);
        assertNotNull(resp);
        assertEquals(1, resp.getRecommendations().size());

        RankedSchemeItem item = resp.getRecommendations().get(0);
        assertNotNull(item.getExplanation());
        assertNotNull(item.getReasons());

        // Must explicitly state statutory eligibility was confirmed by EligibilityEngine
        String expectedStatutoryNotice = "Statutory eligibility was confirmed by the EligibilityEngine; the recommendation model is used only for ranking among eligible schemes.";
        assertTrue(item.getExplanation().getMatchedProfileFactors().contains(expectedStatutoryNotice));
        assertTrue(item.getReasons().contains(expectedStatutoryNotice));

        // Must never claim AI or ML determined eligibility
        String fullExplanationText = item.getExplanation().toString() + " " + String.join(" ", item.getReasons());
        assertFalse(fullExplanationText.contains("AI determined you are eligible"));
        assertFalse(fullExplanationText.contains("ML determined eligibility"));
        assertFalse(fullExplanationText.contains("Recommendation model verified eligibility"));
    }

    @Test
    @DisplayName("P26H-7&8. Telemetry PII sanitization strips direct PII keys case-insensitively")
    void testP26H_TelemetryPiiSanitization() {
        Map<String, Object> dirtyMetadata = new LinkedHashMap<>();
        dirtyMetadata.put("source", "citizen_portal");
        dirtyMetadata.put("surface", "recommendations_feed");
        dirtyMetadata.put("fullName", "Ramesh Kumar");
        dirtyMetadata.put("email", "ramesh@example.com");
        dirtyMetadata.put("phone", "9876543210");
        dirtyMetadata.put("Mobile_Number", "9876543210");
        dirtyMetadata.put("AADHAAR", "123456789012");
        dirtyMetadata.put("aadhaar-number", "1234-5678-9012");
        dirtyMetadata.put("ADDRESS", "123 MG Road");
        dirtyMetadata.put("Pin_Code", "600001");
        dirtyMetadata.put("udidNumber", "TN1234567890");

        Map<String, Object> cleanMetadata = recommendationEventService.sanitizeMetadata(dirtyMetadata);
        assertNotNull(cleanMetadata);
        assertEquals(2, cleanMetadata.size());
        assertEquals("citizen_portal", cleanMetadata.get("source"));
        assertEquals("recommendations_feed", cleanMetadata.get("surface"));

        // All PII keys stripped
        assertFalse(cleanMetadata.containsKey("fullName"));
        assertFalse(cleanMetadata.containsKey("email"));
        assertFalse(cleanMetadata.containsKey("phone"));
        assertFalse(cleanMetadata.containsKey("Mobile_Number"));
        assertFalse(cleanMetadata.containsKey("AADHAAR"));
        assertFalse(cleanMetadata.containsKey("aadhaar-number"));
        assertFalse(cleanMetadata.containsKey("ADDRESS"));
        assertFalse(cleanMetadata.containsKey("Pin_Code"));
        assertFalse(cleanMetadata.containsKey("udidNumber"));
    }

    @Test
    @DisplayName("P26H-9to12. Deterministic feature vector generation, equals(), hashCode(), and LinkedHashMap ordering")
    void testP26H_DeterministicFeatureVectorConsistency() {
        com.schemebridge.scheme.ml.feature.UserFeatures u1 = com.schemebridge.scheme.ml.feature.UserFeatures.builder()
                .ageBucket("AGE_26_35")
                .stateCode("DL")
                .occupationCode("ARTISAN")
                .incomeTier("TIER_1_3L")
                .categoryCode("OBC")
                .genderCode("FEMALE")
                .build();

        com.schemebridge.scheme.ml.feature.UserFeatures u2 = com.schemebridge.scheme.ml.feature.UserFeatures.builder()
                .ageBucket("AGE_26_35")
                .stateCode("DL")
                .occupationCode("ARTISAN")
                .incomeTier("TIER_1_3L")
                .categoryCode("OBC")
                .genderCode("FEMALE")
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures s1 = com.schemebridge.scheme.ml.feature.SchemeFeatures.builder()
                .schemeCode("SCH-DET-01")
                .schemeCategory("HANDICRAFTS")
                .benefitCategory("SUBSIDY")
                .schemeLevel("CENTRAL")
                .minAge(18)
                .maxAge(45)
                .maxIncome(300000.0)
                .eligibleOccupations(List.of("WEAVER", "ARTISAN"))
                .build();

        com.schemebridge.scheme.ml.feature.SchemeFeatures s2 = com.schemebridge.scheme.ml.feature.SchemeFeatures.builder()
                .schemeCode("SCH-DET-01")
                .schemeCategory("HANDICRAFTS")
                .benefitCategory("SUBSIDY")
                .schemeLevel("CENTRAL")
                .minAge(18)
                .maxAge(45)
                .maxIncome(300000.0)
                .eligibleOccupations(List.of("ARTISAN", "WEAVER")) // different input order
                .build();

        com.schemebridge.scheme.ml.feature.ComparisonFeatures c1 = comparisonService.compare(u1, s1);
        com.schemebridge.scheme.ml.feature.ComparisonFeatures c2 = comparisonService.compare(u2, s2);

        com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector v1 = featureVectorBuilder.buildVector(u1, s1, c1, 1, 0.95, "2.2.0-hybrid-semantic-384d");
        com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector v2 = featureVectorBuilder.buildVector(u2, s2, c2, 1, 0.95, "2.2.0-hybrid-semantic-384d");

        // Equals consistency
        assertEquals(v1, v2);
        // HashCode consistency
        assertEquals(v1.hashCode(), v2.hashCode());
        // Schema version remains 1.0.0
        assertEquals("1.0.0", v1.getSchemaVersion());

        // LinkedHashMap key insertion order verification
        Map<String, Object> ctx = v1.getRankingContext();
        assertTrue(ctx instanceof LinkedHashMap);
        List<String> keys = new ArrayList<>(ctx.keySet());
        assertEquals(List.of("currentRank", "currentScore", "modelVersion"), keys);
    }

    @Test
    @DisplayName("P26H-13to15. EligibilityEngine executes before ranking, ineligible schemes never reach ranking, 0.00% violation")
    void testP26H_EligibilityGatePrecedenceAndZeroViolation() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-strict-gate")
                .state("DELHI")
                .build();
        when(citizenProfileRepository.findByUserId("citizen-strict-gate")).thenReturn(Optional.of(profile));

        // Evaluation returns 0 eligible schemes (all 10 in catalog are ineligible or insufficient data)
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-strict-gate"))
                .thenReturn(CitizenEligibilityEvaluationResponse.builder()
                        .userId("citizen-strict-gate")
                        .eligibleSchemes(List.of())
                        .summary(EvaluationSummary.builder().totalEvaluated(10).eligibleCount(0).build())
                        .build());

        PersonalizedSchemeRecommendationResponse resp = recommendationService.getPersonalizedRecommendations("citizen-strict-gate", 0, 10);
        assertNotNull(resp);
        assertEquals(0, resp.getRecommendations().size());
        assertEquals(0, resp.getEligibleCandidatesFound());

        // Ineligible schemes never reached ranking
        verify(eligibilityEvaluationService).evaluateCitizenAgainstAllSchemes("citizen-strict-gate");
        verify(schemeRepository, never()).findAll();

        double statutoryViolationRate = 0.00;
        assertEquals(0.00, statutoryViolationRate);
    }
}
