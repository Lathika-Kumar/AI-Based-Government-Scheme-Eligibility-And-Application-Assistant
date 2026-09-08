package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 22B: Comprehensive Unit & Safety Tests for Hybrid ML Recommendation.
 * Validates hard eligibility gating, semantic scoring, circuit-breaker fallback, and zero-violation safety.
 */
@ExtendWith(MockitoExtension.class)
class EligibleSchemeRecommendationServiceMlTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SemanticEmbeddingIndexService semanticEmbeddingIndexService;

    private MlRecommenderProperties properties;
    private EligibleSchemeRecommendationService service;

    private CitizenProfile mockProfile;
    private Scheme schemeEligible1;
    private Scheme schemeEligible2;
    private Scheme schemeIneligible;

    @BeforeEach
    void setUp() {
        properties = new MlRecommenderProperties();
        properties.getMl().setEnabled(true);
        properties.getMl().setTimeoutMs(200);
        properties.getMl().setShadowMode(true);
        properties.getMl().setModelVersion("2.2.0-hybrid-semantic-384d");

        service = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                semanticEmbeddingIndexService
        );

        mockProfile = CitizenProfile.builder()
                .userId("citizen_test_101")
                .age(35)
                .gender("Male")
                .state("Maharashtra")
                .occupation("Farmer")
                .isFarmer(true)
                .annualIncome(120000.0)
                .bplStatus(true)
                .socialCategory("OBC")
                .build();

        schemeEligible1 = Scheme.builder()
                .schemeCode("SCH-AGRI-01")
                .slug("pm-kisan-maharashtra")
                .title(MultilingualText.builder().english("Maharashtra Farmer Assistance Scheme").build())
                .shortDescription(MultilingualText.builder().english("Financial assistance for small farmers in Maharashtra").build())
                .category(SchemeCategoryRef.builder().code("AGRI").name("Agriculture").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .beneficiaryType("Farmer")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Annual subsidy").build()).build()))
                .tags(List.of("farmer", "subsidy", "kisan"))
                .build();

        schemeEligible2 = Scheme.builder()
                .schemeCode("SCH-HEALTH-02")
                .slug("ayushman-central")
                .title(MultilingualText.builder().english("Central Health Assurance Scheme").build())
                .shortDescription(MultilingualText.builder().english("Universal health cover for families").build())
                .category(SchemeCategoryRef.builder().code("HLTH").name("Health").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .stateOrUt("ALL")
                .beneficiaryType("Family")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Medical cover").build()).build()))
                .tags(List.of("health", "hospital"))
                .build();

        schemeIneligible = Scheme.builder()
                .schemeCode("SCH-PENSION-99")
                .slug("senior-citizen-pension")
                .title(MultilingualText.builder().english("Senior Citizen Pension Scheme").build())
                .category(SchemeCategoryRef.builder().code("SOC").name("Social Welfare").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Kerala") // Ineligible: wrong state and senior age required
                .build();
    }

    @Test
    @DisplayName("1. Eligible scheme receives valid semantic score and appears in recommendations")
    void testEligibleSchemeReceivesSemanticScore() {
        when(citizenProfileRepository.findByUserId("citizen_test_101")).thenReturn(Optional.of(mockProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-01").slug("pm-kisan-maharashtra").status(EligibilityStatus.ELIGIBLE).build(),
                EligibilityEvaluationResult.builder().schemeCode("SCH-HEALTH-02").slug("ayushman-central").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(2).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen_test_101")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1, schemeEligible2, schemeIneligible));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-01"), any())).thenReturn(0.92);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-HEALTH-02"), any())).thenReturn(0.65);

        PersonalizedSchemeRecommendationResponse res = service.getPersonalizedRecommendations("citizen_test_101", 0, 10);

        assertNotNull(res);
        assertEquals(2, res.getRecommendations().size());
        assertEquals("HYBRID_SEMANTIC", res.getRankingMethod());
        assertFalse(res.getFallbackUsed());
        assertEquals("2.2.0-hybrid-semantic-384d", res.getModelVersion());

        // Agri scheme with higher semantic score and farmer match ranks #1
        RankedSchemeItem topItem = res.getRecommendations().get(0);
        assertEquals("SCH-AGRI-01", topItem.getSchemeCode());
        assertEquals(1, topItem.getRank());
        assertTrue(topItem.getRecommendationScore() > 0.70);
    }

    @Test
    @DisplayName("2 & 3. Ineligible scheme receives score 0 and NEVER appears in recommendations")
    void testIneligibleSchemeNeverAppearsInRecommendations() {
        when(citizenProfileRepository.findByUserId("citizen_test_101")).thenReturn(Optional.of(mockProfile));

        // Hard gate only returns SCH-AGRI-01 as eligible; SCH-PENSION-99 is strictly excluded
        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-01").slug("pm-kisan-maharashtra").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(10).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen_test_101")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1, schemeIneligible));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-01"), any())).thenReturn(0.88);

        PersonalizedSchemeRecommendationResponse res = service.getPersonalizedRecommendations("citizen_test_101", 0, 10);

        assertEquals(1, res.getRecommendations().size());
        assertEquals("SCH-AGRI-01", res.getRecommendations().get(0).getSchemeCode());

        // Verify ineligible scheme is not present anywhere in returned recommendations
        boolean hasIneligible = res.getRecommendations().stream()
                .anyMatch(r -> r.getSchemeCode().equals("SCH-PENSION-99"));
        assertFalse(hasIneligible, "Statutory ineligible scheme must NEVER appear in recommendations");
    }

    @Test
    @DisplayName("4 & 6. Missing embedding index triggers safe fallback to deterministic engine")
    void testMissingEmbeddingIndexTriggersFallback() {
        when(citizenProfileRepository.findByUserId("citizen_test_101")).thenReturn(Optional.of(mockProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-01").slug("pm-kisan-maharashtra").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(5).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen_test_101")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));
        // Simulate embedding index not available
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(false);

        PersonalizedSchemeRecommendationResponse res = service.getPersonalizedRecommendations("citizen_test_101", 0, 10);

        assertNotNull(res);
        assertEquals(1, res.getRecommendations().size());
        assertEquals("DETERMINISTIC_FALLBACK", res.getRankingMethod());
        assertTrue(res.getFallbackUsed());
    }

    @Test
    @DisplayName("7. ML exception during scoring triggers circuit-breaker fallback")
    void testMlExceptionTriggersCircuitBreakerFallback() {
        when(citizenProfileRepository.findByUserId("citizen_test_101")).thenReturn(Optional.of(mockProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-01").slug("pm-kisan-maharashtra").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(5).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen_test_101")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        // Simulate unexpected runtime exception from semantic index
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(anyString(), any()))
                .thenThrow(new RuntimeException("Simulated Vector Memory Fault"));

        PersonalizedSchemeRecommendationResponse res = service.getPersonalizedRecommendations("citizen_test_101", 0, 10);

        assertNotNull(res);
        assertEquals(1, res.getRecommendations().size());
        assertEquals("DETERMINISTIC_FALLBACK", res.getRankingMethod());
        assertTrue(res.getFallbackUsed());
    }

    @Test
    @DisplayName("9, 10, 11 & 12. Response contains rankingMethod, modelVersion, fallbackUsed and explainable reasons")
    void testResponseContainsCompleteMetadataAndExplanations() {
        when(citizenProfileRepository.findByUserId("citizen_test_101")).thenReturn(Optional.of(mockProfile));

        List<EligibilityEvaluationResult> eligibleList = List.of(
                EligibilityEvaluationResult.builder().schemeCode("SCH-AGRI-01").slug("pm-kisan-maharashtra").status(EligibilityStatus.ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse evalRes = CitizenEligibilityEvaluationResponse.builder()
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(1).eligibleCount(1).build())
                .eligibleSchemes(eligibleList)
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("citizen_test_101")).thenReturn(evalRes);
        when(schemeRepository.findAll()).thenReturn(List.of(schemeEligible1));
        when(semanticEmbeddingIndexService.isAvailable()).thenReturn(true);
        when(semanticEmbeddingIndexService.computeSemanticSimilarity(eq("SCH-AGRI-01"), any())).thenReturn(0.89);

        PersonalizedSchemeRecommendationResponse res = service.getPersonalizedRecommendations("citizen_test_101", 0, 10);

        assertEquals("HYBRID_SEMANTIC", res.getRankingMethod());
        assertEquals("2.2.0-hybrid-semantic-384d", res.getModelVersion());
        assertFalse(res.getFallbackUsed());

        RankedSchemeItem item = res.getRecommendations().get(0);
        assertNotNull(item.getReasons());
        assertFalse(item.getReasons().isEmpty());
        // Verify reasons are honest and non-sensational
        boolean hasCompliantReason = item.getReasons().stream()
                .anyMatch(r -> r.startsWith("Recommended because"));
        assertTrue(hasCompliantReason);
    }
}
