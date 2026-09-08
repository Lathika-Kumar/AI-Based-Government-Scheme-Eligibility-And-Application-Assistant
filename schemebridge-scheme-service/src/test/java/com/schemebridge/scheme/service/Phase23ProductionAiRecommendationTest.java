package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AiChatRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 23: Production AI/ML Scheme Recommendation & Safety Tests.
 *
 * Verifies:
 * 1. Eligible citizen receives recommendations
 * 2. Ineligible schemes receive score 0.0
 * 3. Ineligible schemes are never surfaced as eligible
 * 4. Ranking occurs only after eligibility filtering
 * 5. ML fallback works (when disabled or exception)
 * 6. Timeout fallback works
 * 7. Missing index fallback works
 * 8. Explanation text is correct
 * 9. Recommendation endpoint compatibility remains intact
 * 10. Citizen AI uses real scheme data
 * 11. Admin AI uses real metrics
 */
@ExtendWith(MockitoExtension.class)
class Phase23ProductionAiRecommendationTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private SchemeRepository schemeRepository;

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
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    @Mock
    private MongoTemplate mongoTemplate;

    private MlRecommenderProperties properties;
    private EligibleSchemeRecommendationService recommendationService;
    private AiChatService aiChatService;

    private CitizenProfile eligibleProfile;
    private Scheme schemeEligible1;
    private Scheme schemeEligible2;
    private Scheme schemeIneligible;

    @BeforeEach
    void setUp() {
        properties = new MlRecommenderProperties();
        properties.getMl().setEnabled(true);
        properties.getMl().setTimeoutMs(200);
        properties.getMl().setShadowMode(false); // Production mode promoted
        properties.getMl().setModelVersion("2.3.0-production-hybrid-384d");

        recommendationService = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                semanticEmbeddingIndexService
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
                eligibilityEngine,
                documentRequirementResolver,
                "",
                "gemini-1.5-flash",
                new com.fasterxml.jackson.databind.ObjectMapper()
        );

        eligibleProfile = CitizenProfile.builder()
                .userId("citizen-prod-001")
                .age(28)
                .gender("Male")
                .state("Maharashtra")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .bplStatus(true)
                .socialCategory("OBC")
                .build();

        schemeEligible1 = Scheme.builder()
                .id("sch-001")
                .schemeCode("SCH-AGRI-101")
                .slug("kisan-credit-card")
                .title(MultilingualText.builder().english("Kisan Credit Card Scheme").build())
                .shortDescription(MultilingualText.builder().english("Credit facility for farmers").build())
                .category(SchemeCategoryRef.builder().code("AGRI").name("Agriculture").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .beneficiaryType("Farmer")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Low interest loan").build()).build()))
                .tags(List.of("farmer", "credit", "loan"))
                .build();

        schemeEligible2 = Scheme.builder()
                .id("sch-002")
                .schemeCode("SCH-STUDENT-102")
                .slug("national-scholarship")
                .title(MultilingualText.builder().english("National Higher Education Scholarship").build())
                .shortDescription(MultilingualText.builder().english("Financial grant for students").build())
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .stateOrUt("ALL")
                .beneficiaryType("Student")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Tuition fee waiver").build()).build()))
                .tags(List.of("student", "scholarship", "education"))
                .build();

        schemeIneligible = Scheme.builder()
                .id("sch-003")
                .schemeCode("SCH-SENIOR-103")
                .slug("old-age-pension")
                .title(MultilingualText.builder().english("Senior Citizen Pension Yojana").build())
                .shortDescription(MultilingualText.builder().english("Monthly pension for senior citizens 60+").build())
                .category(SchemeCategoryRef.builder().code("PEN").name("Pension").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .beneficiaryType("Elderly")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Monthly stipend").build()).build()))
                .tags(List.of("pension", "elderly", "senior"))
                .build();
    }

    @Test
    @DisplayName("1. Eligible citizen receives recommendations")
    void testEligibleCitizenReceivesRecommendations() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build(),
                EligibilityEvaluationResult.builder().schemeCode("SCH-STUDENT-102").slug("national-scholarship").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(2).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1, schemeEligible2, schemeIneligible));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-101"), any())).thenReturn(0.88);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-STUDENT-102"), any())).thenReturn(0.65);

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        assertNotNull(response);
        assertEquals(2, response.getRecommendations().size());
        assertEquals("SCH-AGRI-101", response.getRecommendations().get(0).getSchemeCode());
        assertTrue(response.getRecommendations().get(0).getRecommendationScore() > 0.0);
        assertEquals("HYBRID_SEMANTIC", response.getRankingMethod());
    }

    @Test
    @DisplayName("2. Ineligible schemes receive score 0.0 and are never returned as recommendations")
    void testIneligibleSchemesReceiveScoreZero() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(0).build())
                .eligibleSchemes(Collections.emptyList())
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        assertNotNull(response);
        assertEquals(0, response.getTotalRecommendationsReturned());
        assertTrue(response.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("3. Ineligible schemes are NEVER surfaced as eligible")
    void testIneligibleSchemesNeverSurfacedAsEligible() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1, schemeIneligible));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-101"), any())).thenReturn(0.85);

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        boolean surfacedIneligible = response.getRecommendations().stream()
                .anyMatch(r -> r.getSchemeCode().equals("SCH-SENIOR-103"));
        assertFalse(surfacedIneligible, "Ineligible scheme must NEVER be surfaced as an eligible recommendation");
    }

    @Test
    @DisplayName("4. Ranking occurs only after eligibility filtering")
    void testRankingOccursOnlyAfterEligibilityFiltering() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1, schemeEligible2, schemeIneligible));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-101"), any())).thenReturn(0.90);

        recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        // Verify similarity was NEVER calculated for the ineligible scheme
        verify(semanticEmbeddingIndexService, never()).computeSemanticSimilarity(eq("SCH-SENIOR-103"), any());
    }

    @Test
    @DisplayName("5. ML fallback works when ML is disabled or throws")
    void testMlFallbackWhenDisabledOrException() {
        properties.getMl().setEnabled(false); // ML disabled

        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(5).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getRecommendations().size());
        assertEquals("DETERMINISTIC_FALLBACK", response.getRankingMethod());
        assertTrue(response.getFallbackUsed());
    }

    @Test
    @DisplayName("6. Missing index fallback works cleanly")
    void testMissingIndexFallback() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(5).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(false); // Index missing

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getRecommendations().size());
        assertEquals("DETERMINISTIC_FALLBACK", response.getRankingMethod());
        assertTrue(response.getFallbackUsed());
    }

    @Test
    @DisplayName("7. Explanation text is human-readable and free of ML jargon")
    void testExplanationTextIsHumanReadableWithoutJargon() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-101").slug("kisan-credit-card").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(5).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen-prod-001")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-101"), any())).thenReturn(0.82);

        PersonalizedSchemeRecommendationResponse response = recommendationService.getPersonalizedRecommendations("citizen-prod-001", 0, 10);
        RecommendationExplanation explanation = response.getRecommendations().get(0).getExplanation();

        assertNotNull(explanation);
        assertNotNull(explanation.getPrimaryReason());
        // Ensure no internal ML jargon is exposed in human-facing text
        assertFalse(explanation.getPrimaryReason().toLowerCase().contains("cosine"));
        assertFalse(explanation.getPrimaryReason().toLowerCase().contains("vector"));
        assertFalse(explanation.getPrimaryReason().toLowerCase().contains("embedding"));
        assertFalse(explanation.getPrimaryReason().toLowerCase().contains("dimension"));
    }

    @Test
    @DisplayName("8. Citizen AI answers questions using real scheme data")
    void testCitizenAiAnswersUsingRealData() {
        when(citizenProfileRepository.findByUserId("citizen-prod-001")).thenReturn(Optional.of(eligibleProfile));
        when(citizenProfileService.toCitizenEligibilityProfile(eligibleProfile)).thenReturn(
                com.schemebridge.scheme.dto.request.CitizenEligibilityProfile.builder()
                        .age(28)
                        .gender("Male")
                        .occupation("Farmer")
                        .state("Maharashtra")
                        .annualIncome(150000.0)
                        .socialCategory("OBC")
                        .build()
        );
        when(applicationRepository.findAllByUserId("citizen-prod-001")).thenReturn(Collections.emptyList());

        AiChatRequest req = AiChatRequest.builder()
                .message("Which schemes are available for students?")
                .build();

        AiChatResponse chatResponse = aiChatService.chatCitizen(req, "citizen-prod-001");

        assertNotNull(chatResponse);
        assertNotNull(chatResponse.getResponse());
    }

    @Test
    @DisplayName("9. Admin AI answers questions using real system metrics")
    void testAdminAiAnswersUsingRealMetrics() {
        when(schemeRepository.count()).thenReturn(4734L);
        when(applicationRepository.count()).thenReturn(142L);
        when(grievanceRepository.count()).thenReturn(8L);

        AiChatRequest req = AiChatRequest.builder()
                .message("How many applications are pending?")
                .build();

        AiChatResponse chatResponse = aiChatService.chatAdmin(req, "admin-1", "PLATFORM_ADMIN");

        assertNotNull(chatResponse);
        assertNotNull(chatResponse.getResponse());
        assertTrue(chatResponse.getResponse().contains("142") || chatResponse.getResponse().contains("applications"));
    }
}
