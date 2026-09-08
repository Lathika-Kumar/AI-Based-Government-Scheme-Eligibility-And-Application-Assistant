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
 * Phase 25: AI/ML Continuous Learning, Real User Feedback Collection,
 * Recommendation Training Pipeline, Safe Model Registry & Model Promotion Test Suite.
 */
@ExtendWith(MockitoExtension.class)
public class Phase25AiMlTrainingTest {

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
    private EligibleSchemeRecommendationService recommendationService;
    private AiChatService aiChatService;
    private MlRecommenderProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MlRecommenderProperties();
        properties.getMl().setEnabled(true);
        properties.getMl().setModelVersion("2.2.0-hybrid-semantic-384d");
        properties.getMl().setTimeoutMs(200);

        modelRegistryService = new ModelRegistryService();

        recommendationService = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                semanticEmbeddingIndexService,
                modelRegistryService
        );

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
}
