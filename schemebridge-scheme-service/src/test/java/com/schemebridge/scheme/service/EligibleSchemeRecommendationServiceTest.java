package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibleSchemeRecommendationServiceTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private EligibleSchemeRecommendationService recommendationService;

    private CitizenProfile farmerProfile;
    private Scheme kisanScheme;
    private Scheme stateScheme;
    private Scheme genericScheme;

    @BeforeEach
    void setUp() {
        farmerProfile = CitizenProfile.builder()
                .userId("65")
                .state("Maharashtra")
                .occupation("Farmer")
                .isFarmer(true)
                .annualIncome(160000.0)
                .socialCategory("OBC")
                .age(34)
                .gender("Male")
                .bplStatus(true)
                .build();

        kisanScheme = Scheme.builder()
                .schemeCode("PM_KISAN_2026")
                .slug("pm-kisan-samman-nidhi")
                .title(MultilingualText.builder().english("PM-KISAN Samman Nidhi").build())
                .shortDescription(MultilingualText.builder().english("Direct financial income support to farmers").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture & Rural").build())
                .beneficiaryType("FARMER")
                .schemeType("DIRECT_BENEFIT")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Rs 6,000 per year").build()).build()))
                .tags(List.of("farmer", "agriculture"))
                .build();

        stateScheme = Scheme.builder()
                .schemeCode("MH_AGRI_01")
                .slug("maharashtra-farmer-aid")
                .title(MultilingualText.builder().english("Maharashtra Farmer Solar Pump Scheme").build())
                .shortDescription(MultilingualText.builder().english("State subsidy for solar water pumps").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture & Rural").build())
                .beneficiaryType("FARMER")
                .schemeType("SUBSIDY")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("90% solar pump subsidy").build()).build()))
                .tags(List.of("farmer", "maharashtra"))
                .build();

        genericScheme = Scheme.builder()
                .schemeCode("GEN_AID_01")
                .slug("general-citizen-aid")
                .title(MultilingualText.builder().english("National Citizen Welfare Grant").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("SOCIAL_WELFARE").name("Social Welfare").build())
                .beneficiaryType("INDIVIDUAL")
                .build();
    }

    @Test
    @DisplayName("Phase 7: Ranks pre-validated eligible schemes by multi-criteria utility score")
    void testRankEligibleSchemes_Success() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerProfile));

        EligibilityEvaluationResult resKisan = EligibilityEvaluationResult.builder()
                .schemeCode("PM_KISAN_2026")
                .slug("pm-kisan-samman-nidhi")
                .schemeTitle("PM-KISAN Samman Nidhi")
                .schemeLevel("CENTRAL")
                .categoryCode("AGRICULTURE")
                .categoryName("Agriculture & Rural")
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .passedConditions(List.of("National Central Scheme", "Farmer requirement met"))
                .build();

        EligibilityEvaluationResult resState = EligibilityEvaluationResult.builder()
                .schemeCode("MH_AGRI_01")
                .slug("maharashtra-farmer-aid")
                .schemeTitle("Maharashtra Farmer Solar Pump Scheme")
                .schemeLevel("STATE")
                .categoryCode("AGRICULTURE")
                .categoryName("Agriculture & Rural")
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .passedConditions(List.of("State residency verified: Maharashtra", "Farmer requirement met"))
                .build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder()
                        .totalEvaluated(794)
                        .eligibleCount(2)
                        .insufficientDataCount(700)
                        .notEligibleCount(92)
                        .build())
                .eligibleSchemes(List.of(resKisan, resState))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(kisanScheme, stateScheme, genericScheme));

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("65", 0, 10);

        assertNotNull(response);
        assertEquals("65", response.getUserId());
        assertEquals("Maharashtra", response.getCitizenState());
        assertEquals(794, response.getTotalCatalogEvaluated());
        assertEquals(2, response.getEligibleCandidatesFound());
        assertEquals(2, response.getTotalRecommendationsReturned());

        List<RankedSchemeItem> items = response.getRecommendations();
        assertEquals(2, items.size());

        // State scheme should score highest due to local state match (1.0) + farmer alignment (1.0) + BPL
        RankedSchemeItem topRanked = items.get(0);
        assertEquals(1, topRanked.getRank());
        assertEquals("MH_AGRI_01", topRanked.getSchemeCode());
        assertEquals("ELIGIBLE", topRanked.getEligibilityStatus());
        assertTrue(topRanked.getRecommendationScore() > 0.85);

        assertNotNull(topRanked.getExplanation());
        assertNotNull(topRanked.getExplanation().getPrimaryReason());
        assertFalse(topRanked.getExplanation().getMatchedProfileFactors().isEmpty());
    }

    @Test
    @DisplayName("Phase 7: Returns empty list gracefully when zero schemes are eligible")
    void testNoEligibleSchemes_ReturnsEmptyList() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerProfile));

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder()
                        .totalEvaluated(794)
                        .eligibleCount(0)
                        .insufficientDataCount(700)
                        .notEligibleCount(94)
                        .build())
                .eligibleSchemes(List.of())
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("65", 0, 10);

        assertNotNull(response);
        assertEquals(0, response.getEligibleCandidatesFound());
        assertEquals(0, response.getTotalRecommendationsReturned());
        assertTrue(response.getRecommendations().isEmpty());
    }
}
