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

/**
 * Phase 8: Real-Data Recommendation Engine & Hybrid MADM + Semantic Vector Space Validation.
 * Validates deterministic gating, semantic feature scoring, rank ordering,
 * explainability payloads, and multi-scenario citizen personas.
 */
@ExtendWith(MockitoExtension.class)
class RealDataRecommendationValidationTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private EligibleSchemeRecommendationService recommendationService;

    private CitizenProfile farmerCitizenA;
    private CitizenProfile studentCitizenB;
    private CitizenProfile lowIncomeCitizenD;
    private CitizenProfile incompleteCitizenG;

    private Scheme pmKisanCentral;
    private Scheme mhSolarPumpState;
    private Scheme tnHigherEduScholarship;
    private Scheme genericWelfareAid;

    @BeforeEach
    void setUp() {
        // Real Citizen Personas (based on MongoDB records)
        farmerCitizenA = CitizenProfile.builder()
                .userId("65")
                .state("Maharashtra")
                .age(34)
                .occupation("Farmer")
                .isFarmer(true)
                .annualIncome(160000.0)
                .socialCategory("OBC")
                .bplStatus(true)
                .disabilityStatus(false)
                .build();

        studentCitizenB = CitizenProfile.builder()
                .userId("82")
                .state("Tamil Nadu")
                .age(19)
                .occupation("Student")
                .isStudent(true)
                .annualIncome(72000.0)
                .socialCategory("OBC")
                .bplStatus(false)
                .disabilityStatus(false)
                .build();

        lowIncomeCitizenD = CitizenProfile.builder()
                .userId("53")
                .state("Maharashtra")
                .age(22)
                .occupation("Farmer")
                .isFarmer(true)
                .annualIncome(120000.0)
                .socialCategory("General")
                .bplStatus(true)
                .disabilityStatus(false)
                .build();

        incompleteCitizenG = CitizenProfile.builder()
                .userId("test_citizen_inc_1787739621922")
                .age(22)
                .build();

        // Real Scheme Catalog Records
        pmKisanCentral = Scheme.builder()
                .schemeCode("PM_KISAN_2026")
                .slug("pm-kisan-samman-nidhi")
                .title(MultilingualText.builder().english("PM-KISAN Samman Nidhi").build())
                .shortDescription(MultilingualText.builder().english("Direct financial income support to farmers").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture & Rural").build())
                .beneficiaryType("FARMER")
                .schemeType("DIRECT_BENEFIT")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Rs 6,000 per year in 3 equal installments").build()).build()))
                .tags(List.of("farmer", "agriculture", "kisan"))
                .build();

        mhSolarPumpState = Scheme.builder()
                .schemeCode("MH_AGRI_01")
                .slug("maharashtra-farmer-solar-pump")
                .title(MultilingualText.builder().english("Maharashtra Farmer Solar Pump Scheme").build())
                .shortDescription(MultilingualText.builder().english("State subsidy for agricultural solar water pumps").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture & Rural").build())
                .beneficiaryType("FARMER")
                .schemeType("SUBSIDY")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("90% solar pump subsidy").build()).build()))
                .tags(List.of("farmer", "maharashtra", "irrigation"))
                .build();

        tnHigherEduScholarship = Scheme.builder()
                .schemeCode("TN_EDU_01")
                .slug("tamil-nadu-higher-education-scholarship")
                .title(MultilingualText.builder().english("Tamil Nadu Higher Education Scholarship").build())
                .shortDescription(MultilingualText.builder().english("Scholarship for college students in Tamil Nadu").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .category(SchemeCategoryRef.builder().code("EDUCATION").name("Education & Learning").build())
                .beneficiaryType("STUDENT")
                .schemeType("SCHOLARSHIP")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Tuition fee waiver and monthly stipend").build()).build()))
                .tags(List.of("student", "education", "scholarship"))
                .build();

        genericWelfareAid = Scheme.builder()
                .schemeCode("GEN_AID_01")
                .slug("national-citizen-welfare-grant")
                .title(MultilingualText.builder().english("National Citizen Welfare Grant").build())
                .shortDescription(MultilingualText.builder().english("Financial assistance for low-income citizens").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("SOCIAL_WELFARE").name("Social Welfare").build())
                .beneficiaryType("INDIVIDUAL")
                .schemeType("WELFARE")
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("One-time welfare grant").build()).build()))
                .tags(List.of("welfare", "financial_aid"))
                .build();
    }

    // ── 1. Critical Gate Test (Invariant Validation) ───────────────────────────

    @Test
    @DisplayName("Critical Gate Test: NOT_ELIGIBLE and INSUFFICIENT_DATA schemes are strictly rejected")
    void testCriticalGate_OnlyEligibleSchemesEnterRanking() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerCitizenA));

        EligibilityEvaluationResult eligibleRes = EligibilityEvaluationResult.builder()
                .schemeCode("PM_KISAN_2026")
                .schemeTitle("PM-KISAN Samman Nidhi")
                .schemeLevel("CENTRAL")
                .categoryCode("AGRICULTURE")
                .categoryName("Agriculture & Rural")
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .passedConditions(List.of("Central Scheme: National", "Farmer requirement passed"))
                .build();

        EligibilityEvaluationResult notEligibleRes = EligibilityEvaluationResult.builder()
                .schemeCode("TN_EDU_01")
                .schemeTitle("Tamil Nadu Higher Education Scholarship")
                .schemeLevel("STATE")
                .categoryCode("EDUCATION")
                .status(EligibilityStatus.NOT_ELIGIBLE)
                .confidenceScore(1.0)
                .failedConditions(List.of("State residency mismatch: Maharashtra != Tamil Nadu"))
                .build();

        EligibilityEvaluationResult insufficientRes = EligibilityEvaluationResult.builder()
                .schemeCode("NARRATIVE_01")
                .schemeTitle("Narrative Only Scheme")
                .schemeLevel("CENTRAL")
                .status(EligibilityStatus.INSUFFICIENT_DATA)
                .confidenceScore(0.0)
                .missingAttributes(List.of("narrativeEligibilityCriteria"))
                .build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder()
                        .totalEvaluated(3)
                        .eligibleCount(1)
                        .insufficientDataCount(1)
                        .notEligibleCount(1)
                        .build())
                .eligibleSchemes(List.of(eligibleRes))
                .insufficientDataSchemes(List.of(insufficientRes))
                .notEligibleSchemes(List.of(notEligibleRes))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(pmKisanCentral, tnHigherEduScholarship));

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("65", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getEligibleCandidatesFound());
        assertEquals(1, response.getTotalRecommendationsReturned());

        List<RankedSchemeItem> items = response.getRecommendations();
        assertEquals(1, items.size());

        RankedSchemeItem item = items.get(0);
        assertEquals("PM_KISAN_2026", item.getSchemeCode());
        assertEquals("ELIGIBLE", item.getEligibilityStatus());

        // Invariant Assertion: Neither TN_EDU_01 (NOT_ELIGIBLE) nor NARRATIVE_01 (INSUFFICIENT_DATA) appears
        assertTrue(items.stream().noneMatch(i -> "TN_EDU_01".equals(i.getSchemeCode())));
        assertTrue(items.stream().noneMatch(i -> "NARRATIVE_01".equals(i.getSchemeCode())));
    }

    // ── 2. Hybrid MADM + Semantic Mathematical Score Verification ─────────────

    @Test
    @DisplayName("MADM + Semantic Mathematical Score Verification: Exactly verifies weighted formula")
    void testMADM_MathematicalScoreCalculation() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerCitizenA));

        // For Farmer Citizen A (Income 160000, BPL true, State Maharashtra, Farmer true):
        // Scheme: MH_AGRI_01 (State scheme in Maharashtra, Agriculture, Farmer, Direct benefits, Tags: farmer, ma)
        // Expected component values:
        // f_occ = 1.0 (Farmer match in Agri category)
        // f_econ = 0.50 + 0.30 (BPL) + 0.20 * (1 - 160000/300000) = 0.50 + 0.30 + 0.20 * 0.4667 = 0.8933
        // f_geo = 1.0 (State scheme matching Maharashtra)
        // f_benefit = 0.90 (Direct structured benefits)
        // f_semantic = 1.0 (>= 3 matches: farmer, agriculture, maharashtra)
        // Expected Raw Score:
        // S = 0.25*(1.0) + 0.25*(0.8933) + 0.20*(1.0) + 0.15*(0.90) + 0.15*(1.0)
        // S = 0.2500 + 0.2233 + 0.2000 + 0.1350 + 0.1500 = 0.9583

        EligibilityEvaluationResult mhRes = EligibilityEvaluationResult.builder()
                .schemeCode("MH_AGRI_01")
                .schemeTitle("Maharashtra Farmer Solar Pump Scheme")
                .schemeLevel("STATE")
                .categoryCode("AGRICULTURE")
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .passedConditions(List.of("State residency verified: Maharashtra", "Farmer requirement met"))
                .build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder()
                        .totalEvaluated(1).eligibleCount(1).build())
                .eligibleSchemes(List.of(mhRes))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(mhSolarPumpState));

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("65", 0, 10);

        RankedSchemeItem item = response.getRecommendations().get(0);
        double actualScore = item.getRecommendationScore();

        // Mathematical verification within tolerance
        assertEquals(0.9583, actualScore, 0.005);
        assertTrue(actualScore >= 0.95 && actualScore <= 0.97);
    }

    // ── 3. Scenario A: Farmer Persona (Maharashtra) ───────────────────────────

    @Test
    @DisplayName("Scenario A: Real Farmer in Maharashtra prioritizes local Agricultural Subsidies")
    void testScenarioA_FarmerPersona() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerCitizenA));

        EligibilityEvaluationResult kisanRes = EligibilityEvaluationResult.builder()
                .schemeCode("PM_KISAN_2026").schemeTitle("PM-KISAN Samman Nidhi")
                .schemeLevel("CENTRAL").categoryCode("AGRICULTURE").status(EligibilityStatus.ELIGIBLE).build();

        EligibilityEvaluationResult mhRes = EligibilityEvaluationResult.builder()
                .schemeCode("MH_AGRI_01").schemeTitle("Maharashtra Farmer Solar Pump Scheme")
                .schemeLevel("STATE").categoryCode("AGRICULTURE").status(EligibilityStatus.ELIGIBLE).build();

        EligibilityEvaluationResult genRes = EligibilityEvaluationResult.builder()
                .schemeCode("GEN_AID_01").schemeTitle("National Citizen Welfare Grant")
                .schemeLevel("CENTRAL").categoryCode("SOCIAL_WELFARE").status(EligibilityStatus.ELIGIBLE).build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65").citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(3).build())
                .eligibleSchemes(List.of(kisanRes, mhRes, genRes))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(pmKisanCentral, mhSolarPumpState, genericWelfareAid));

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("65", 0, 10);

        List<RankedSchemeItem> recs = response.getRecommendations();
        assertEquals(3, recs.size());

        // Rank 1: Maharashtra Solar Pump (Local state scheme + Farmer match)
        assertEquals(1, recs.get(0).getRank());
        assertEquals("MH_AGRI_01", recs.get(0).getSchemeCode());

        // Rank 2: PM-KISAN (National central scheme + Farmer match)
        assertEquals(2, recs.get(1).getRank());
        assertEquals("PM_KISAN_2026", recs.get(1).getSchemeCode());

        // Rank 3: Generic Welfare (No occupational affinity)
        assertEquals(3, recs.get(2).getRank());
        assertEquals("GEN_AID_01", recs.get(2).getSchemeCode());

        assertTrue(recs.get(0).getRecommendationScore() > recs.get(1).getRecommendationScore());
        assertTrue(recs.get(1).getRecommendationScore() > recs.get(2).getRecommendationScore());
    }

    // ── 4. Scenario B: Student Persona (Tamil Nadu) ───────────────────────────

    @Test
    @DisplayName("Scenario B: Real Student in Tamil Nadu prioritizes Higher Education Scholarships")
    void testScenarioB_StudentPersona() {
        when(citizenProfileRepository.findByUserId("82")).thenReturn(Optional.of(studentCitizenB));

        EligibilityEvaluationResult tnEduRes = EligibilityEvaluationResult.builder()
                .schemeCode("TN_EDU_01").schemeTitle("Tamil Nadu Higher Education Scholarship")
                .schemeLevel("STATE").categoryCode("EDUCATION").status(EligibilityStatus.ELIGIBLE).build();

        EligibilityEvaluationResult genRes = EligibilityEvaluationResult.builder()
                .schemeCode("GEN_AID_01").schemeTitle("National Citizen Welfare Grant")
                .schemeLevel("CENTRAL").categoryCode("SOCIAL_WELFARE").status(EligibilityStatus.ELIGIBLE).build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("82").citizenState("Tamil Nadu")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(2).eligibleCount(2).build())
                .eligibleSchemes(List.of(tnEduRes, genRes))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("82")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(tnHigherEduScholarship, genericWelfareAid));

        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations("82", 0, 10);

        List<RankedSchemeItem> recs = response.getRecommendations();
        assertEquals(2, recs.size());

        // Rank 1 must be Tamil Nadu Education Scholarship
        assertEquals(1, recs.get(0).getRank());
        assertEquals("TN_EDU_01", recs.get(0).getSchemeCode());
        assertTrue(recs.get(0).getRecommendationScore() > recs.get(1).getRecommendationScore());

        // Check Explainability
        assertNotNull(recs.get(0).getExplanation());
        assertTrue(recs.get(0).getExplanation().getPrimaryReason().contains("Student") || recs.get(0).getExplanation().getPrimaryReason().contains("occupation"));
    }

    // ── 5. Pagination & Deterministic Sorting ─────────────────────────────────

    @Test
    @DisplayName("Pagination Validation: Zero duplicate schemes and continuous monotonic rank across pages")
    void testPagination_StableAndContinuous() {
        when(citizenProfileRepository.findByUserId("65")).thenReturn(Optional.of(farmerCitizenA));

        EligibilityEvaluationResult r1 = EligibilityEvaluationResult.builder().schemeCode("MH_AGRI_01").status(EligibilityStatus.ELIGIBLE).build();
        EligibilityEvaluationResult r2 = EligibilityEvaluationResult.builder().schemeCode("PM_KISAN_2026").status(EligibilityStatus.ELIGIBLE).build();
        EligibilityEvaluationResult r3 = EligibilityEvaluationResult.builder().schemeCode("GEN_AID_01").status(EligibilityStatus.ELIGIBLE).build();

        CitizenEligibilityEvaluationResponse evalResponse = CitizenEligibilityEvaluationResponse.builder()
                .userId("65").citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(3).build())
                .eligibleSchemes(List.of(r1, r2, r3))
                .build();

        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("65")).thenReturn(evalResponse);
        when(schemeRepository.findAll()).thenReturn(List.of(pmKisanCentral, mhSolarPumpState, genericWelfareAid));

        // Page 0 (size 2)
        PersonalizedSchemeRecommendationResponse page0 = recommendationService.getPersonalizedRecommendations("65", 0, 2);
        assertEquals(2, page0.getRecommendations().size());
        assertEquals(1, page0.getRecommendations().get(0).getRank());
        assertEquals(2, page0.getRecommendations().get(1).getRank());
        String code0_0 = page0.getRecommendations().get(0).getSchemeCode();
        String code0_1 = page0.getRecommendations().get(1).getSchemeCode();

        // Page 1 (size 2)
        PersonalizedSchemeRecommendationResponse page1 = recommendationService.getPersonalizedRecommendations("65", 1, 2);
        assertEquals(1, page1.getRecommendations().size());
        assertEquals(3, page1.getRecommendations().get(0).getRank());
        String code1_0 = page1.getRecommendations().get(0).getSchemeCode();

        // Invariant: Zero duplicate schemes between page 0 and page 1
        assertNotEquals(code0_0, code1_0);
        assertNotEquals(code0_1, code1_0);
    }
}
