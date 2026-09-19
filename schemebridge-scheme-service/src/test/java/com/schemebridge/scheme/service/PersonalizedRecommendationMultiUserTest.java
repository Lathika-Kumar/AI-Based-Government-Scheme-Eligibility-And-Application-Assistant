package com.schemebridge.scheme.service;

import com.schemebridge.scheme.config.MlRecommenderProperties;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.ml.feature.FeatureVectorBuilder;
import com.schemebridge.scheme.ml.feature.SchemeFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserFeatureExtractor;
import com.schemebridge.scheme.ml.feature.UserSchemeComparisonService;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import com.schemebridge.scheme.repository.SchemeVerifiedDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Multi-User Personalization & Isolation Verification Test.
 *
 * Invariants Verified:
 *   1. Different citizens with distinct attributes (Maharashtra Farmer vs Tamil Nadu Student vs Delhi Entrepreneur)
 *      MUST receive distinct personalized recommendation lists.
 *   2. Occupational, geographic, and economic attributes drive meaningful differentiation in recommendation scores.
 *   3. Scores must NEVER collapse into a monolithic identical 0.7667 score sorted alphabetically.
 *   4. Zero fabrication of historical user interaction data or behavioral ratings.
 */
@ExtendWith(MockitoExtension.class)
class PersonalizedRecommendationMultiUserTest {

    @Mock
    private EligibilityEvaluationService eligibilityEvaluationService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    private EligibleSchemeRecommendationService recommendationService;

    private CitizenProfile farmerMaharashtra;
    private CitizenProfile studentTamilNadu;
    private CitizenProfile businessDelhi;

    private Scheme mhAgriScheme;
    private Scheme tnEduScheme;
    private Scheme centralMudrasScheme;

    @BeforeEach
    void setUp() {
        UserFeatureExtractor userFeatureExtractor = new UserFeatureExtractor();
        SchemeFeatureExtractor schemeFeatureExtractor = new SchemeFeatureExtractor();
        UserSchemeComparisonService userSchemeComparisonService = new UserSchemeComparisonService();
        FeatureVectorBuilder featureVectorBuilder = new FeatureVectorBuilder();
        MlRecommenderProperties properties = new MlRecommenderProperties();

        recommendationService = new EligibleSchemeRecommendationService(
                eligibilityEvaluationService,
                citizenProfileRepository,
                schemeRepository,
                properties,
                null,
                null,
                userFeatureExtractor,
                schemeFeatureExtractor,
                userSchemeComparisonService,
                featureVectorBuilder,
                schemeVerifiedDataRepository,
                null
        );

        // Persona 1: User 65 - Maharashtra Farmer
        farmerMaharashtra = CitizenProfile.builder()
                .userId("user-65")
                .state("Maharashtra")
                .district("Pune")
                .occupation("Farmer")
                .isFarmer(true)
                .isStudent(false)
                .bplStatus(true)
                .annualIncome(160000.0)
                .socialCategory("OBC")
                .age(34)
                .gender("Male")
                .build();

        // Persona 2: User 82 - Tamil Nadu Student
        studentTamilNadu = CitizenProfile.builder()
                .userId("user-82")
                .state("Tamil Nadu")
                .district("Chennai")
                .occupation("Student")
                .isFarmer(false)
                .isStudent(true)
                .bplStatus(false)
                .annualIncome(72000.0)
                .socialCategory("OBC")
                .age(19)
                .gender("Female")
                .build();

        // Persona 3: User 181 - Delhi Self-Employed Entrepreneur
        businessDelhi = CitizenProfile.builder()
                .userId("user-181")
                .state("Delhi")
                .district("New Delhi")
                .occupation("Self-Employed")
                .isFarmer(false)
                .isStudent(false)
                .bplStatus(false)
                .annualIncome(99987.0)
                .socialCategory("General")
                .age(19)
                .gender("Female")
                .build();

        // Scheme 1: Maharashtra Agricultural Solar Pump (State Scheme, Agriculture)
        mhAgriScheme = Scheme.builder()
                .schemeCode("MH-AGRI-001")
                .title(MultilingualText.builder().english("Maharashtra Solar Agriculture Pump Scheme").build())
                .shortDescription(MultilingualText.builder().english("Subsidized solar pumps for farmers in Maharashtra").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .category(SchemeCategoryRef.builder().code("AGRICULTURE").name("Agriculture & Farming").build())
                .beneficiaryType("INDIVIDUAL")
                .tags(List.of("farmer", "solar", "irrigation", "maharashtra"))
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Solar pump subsidy").build()).build()))
                .build();

        // Scheme 2: Tamil Nadu Post-Matric Scholarship (State Scheme, Education)
        tnEduScheme = Scheme.builder()
                .schemeCode("TN-EDU-001")
                .title(MultilingualText.builder().english("Tamil Nadu Higher Education Scholarship").build())
                .shortDescription(MultilingualText.builder().english("Financial stipend for enrolled college students in Tamil Nadu").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .category(SchemeCategoryRef.builder().code("EDUCATION").name("Education & Scholarships").build())
                .beneficiaryType("INDIVIDUAL")
                .tags(List.of("student", "scholarship", "college", "tamil nadu"))
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Tuition fee waiver").build()).build()))
                .build();

        // Scheme 3: Pradhan Mantri MUDRA Yojana (Central Scheme, Business & MSME)
        centralMudrasScheme = Scheme.builder()
                .schemeCode("CEN-MUDRA-001")
                .title(MultilingualText.builder().english("Pradhan Mantri MUDRA Yojana").build())
                .shortDescription(MultilingualText.builder().english("Collateral-free micro enterprise credit for self-employed individuals").build())
                .schemeLevel(SchemeLevel.CENTRAL)
                .category(SchemeCategoryRef.builder().code("BUSINESS").name("Business & Self Employment").build())
                .beneficiaryType("INDIVIDUAL")
                .tags(List.of("self-employed", "business", "loan", "micro-credit"))
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Micro finance up to 10 Lakhs").build()).build()))
                .build();

        when(schemeRepository.findAll()).thenReturn(List.of(mhAgriScheme, tnEduScheme, centralMudrasScheme));
    }

    @Test
    @DisplayName("Verify User A (Farmer MH) vs User B (Student TN) receive distinct top recommendations")
    void testPersonalizedRecommendationsDivergenceBetweenFarmerAndStudent() {
        // Setup User A
        when(citizenProfileRepository.findByUserId("user-65")).thenReturn(Optional.of(farmerMaharashtra));
        CitizenEligibilityEvaluationResponse evalA = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(2).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("MH-AGRI-001").status(EligibilityStatus.ELIGIBLE).build(),
                        EligibilityEvaluationResult.builder().schemeCode("CEN-MUDRA-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-65")).thenReturn(evalA);

        // Setup User B
        when(citizenProfileRepository.findByUserId("user-82")).thenReturn(Optional.of(studentTamilNadu));
        CitizenEligibilityEvaluationResponse evalB = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-82")
                .citizenState("Tamil Nadu")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(2).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("TN-EDU-001").status(EligibilityStatus.ELIGIBLE).build(),
                        EligibilityEvaluationResult.builder().schemeCode("CEN-MUDRA-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-82")).thenReturn(evalB);

        // Execute recommendation for User A
        PersonalizedSchemeRecommendationResponse respA =
                recommendationService.getPersonalizedRecommendations("user-65", 0, 10);

        // Execute recommendation for User B
        PersonalizedSchemeRecommendationResponse respB =
                recommendationService.getPersonalizedRecommendations("user-82", 0, 10);

        // Assertions
        assertNotNull(respA);
        assertNotNull(respB);

        assertEquals(2, respA.getTotalRecommendationsReturned());
        assertEquals(2, respB.getTotalRecommendationsReturned());

        // Top recommendation for Farmer in Maharashtra MUST be the Maharashtra Agricultural scheme
        RankedSchemeItem topItemA = respA.getRecommendations().get(0);
        assertEquals("MH-AGRI-001", topItemA.getSchemeCode());
        assertTrue(topItemA.getRecommendationScore() > 0.85, "Farmer matching agri scheme must have high score");

        // Top recommendation for Student in Tamil Nadu MUST be the Tamil Nadu Education scheme
        RankedSchemeItem topItemB = respB.getRecommendations().get(0);
        assertEquals("TN-EDU-001", topItemB.getSchemeCode());
        assertTrue(topItemB.getRecommendationScore() > 0.85, "Student matching edu scheme must have high score");

        // Crucial: The top recommendations must NOT be identical
        assertNotEquals(topItemA.getSchemeCode(), topItemB.getSchemeCode(),
                "Different citizen personas MUST receive distinct top recommendations");
    }

    @Test
    @DisplayName("Verify User C (Delhi Entrepreneur) receives Central MUDRA scheme prioritized")
    void testPersonalizedRecommendationsForDelhiEntrepreneur() {
        when(citizenProfileRepository.findByUserId("user-181")).thenReturn(Optional.of(businessDelhi));
        CitizenEligibilityEvaluationResponse evalC = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-181")
                .citizenState("Delhi")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(1).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("CEN-MUDRA-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-181")).thenReturn(evalC);

        PersonalizedSchemeRecommendationResponse respC =
                recommendationService.getPersonalizedRecommendations("user-181", 0, 10);

        assertNotNull(respC);
        assertEquals(1, respC.getTotalRecommendationsReturned());
        assertEquals("CEN-MUDRA-001", respC.getRecommendations().get(0).getSchemeCode());
    }

    @Test
    @DisplayName("Verify Citizen C (Artisan in Assam) receives Assam Handloom/Artisan scheme prioritized")
    void testPersonalizedRecommendationsForAssamArtisan() {
        CitizenProfile artisanAssam = CitizenProfile.builder()
                .userId("user-artisan-as")
                .state("Assam")
                .district("Kamrup")
                .occupation("Artisan")
                .isFarmer(false)
                .isStudent(false)
                .bplStatus(false)
                .annualIncome(140000.0)
                .socialCategory("General")
                .age(42)
                .gender("Female")
                .build();

        Scheme assamArtisanScheme = Scheme.builder()
                .schemeCode("AS-ARTISAN-001")
                .title(MultilingualText.builder().english("Assam Handloom and Handicraft Artisan Support Scheme").build())
                .shortDescription(MultilingualText.builder().english("Working capital subsidy and toolkit support for traditional artisans and weavers in Assam").build())
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Assam")
                .category(SchemeCategoryRef.builder().code("HANDICRAFT").name("Handloom & Handicrafts").build())
                .beneficiaryType("INDIVIDUAL")
                .tags(List.of("artisan", "handloom", "weaver", "craft", "assam"))
                .benefits(List.of(SchemeBenefit.builder().description(MultilingualText.builder().english("Rs 25,000 tool subsidy").build()).build()))
                .build();

        when(citizenProfileRepository.findByUserId("user-artisan-as")).thenReturn(Optional.of(artisanAssam));
        when(schemeRepository.findAll()).thenReturn(List.of(mhAgriScheme, tnEduScheme, centralMudrasScheme, assamArtisanScheme));

        CitizenEligibilityEvaluationResponse eval = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-artisan-as")
                .citizenState("Assam")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(4).eligibleCount(2).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("AS-ARTISAN-001").status(EligibilityStatus.ELIGIBLE).build(),
                        EligibilityEvaluationResult.builder().schemeCode("CEN-MUDRA-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-artisan-as")).thenReturn(eval);

        PersonalizedSchemeRecommendationResponse resp =
                recommendationService.getPersonalizedRecommendations("user-artisan-as", 0, 10);

        assertNotNull(resp);
        assertEquals(2, resp.getTotalRecommendationsReturned());
        RankedSchemeItem top = resp.getRecommendations().get(0);
        assertEquals("AS-ARTISAN-001", top.getSchemeCode(), "Assam Artisan scheme must rank #1 for Assam Artisan");
        assertTrue(top.getRecommendationScore() > 0.85);
    }

    @Test
    @DisplayName("Verify Eligibility Violation Rate = 0.00%: Ineligible schemes are strictly excluded from ranking")
    void testEligibilitySafetyGate_ZeroIneligibleSchemesEnterRanking() {
        when(citizenProfileRepository.findByUserId("user-65")).thenReturn(Optional.of(farmerMaharashtra));

        // EligibilityEngine returns: MH-AGRI-001 is ELIGIBLE, but TN-EDU-001 is NOT_ELIGIBLE
        CitizenEligibilityEvaluationResponse eval = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(1).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("MH-AGRI-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-65")).thenReturn(eval);

        PersonalizedSchemeRecommendationResponse resp =
                recommendationService.getPersonalizedRecommendations("user-65", 0, 10);

        assertNotNull(resp);
        assertEquals(1, resp.getTotalRecommendationsReturned());
        for (RankedSchemeItem item : resp.getRecommendations()) {
            assertEquals("ELIGIBLE", item.getEligibilityStatus(), "Every recommended scheme MUST be statutorily ELIGIBLE");
            assertNotEquals("TN-EDU-001", item.getSchemeCode(), "Ineligible scheme must never enter ranking");
        }
    }

    @Test
    @DisplayName("Verify missing attributes do NOT produce artificial positive match points")
    void testMissingAttributesDoNotReceivePositivePoints() {
        // Citizen with missing occupation, state, and income
        CitizenProfile sparseProfile = CitizenProfile.builder()
                .userId("user-sparse")
                .build();

        when(citizenProfileRepository.findByUserId("user-sparse")).thenReturn(Optional.of(sparseProfile));

        CitizenEligibilityEvaluationResponse eval = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-sparse")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(1).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("MH-AGRI-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-sparse")).thenReturn(eval);

        PersonalizedSchemeRecommendationResponse resp =
                recommendationService.getPersonalizedRecommendations("user-sparse", 0, 10);

        assertNotNull(resp);
        RankedSchemeItem item = resp.getRecommendations().get(0);
        // Because state, occupation, income, and demographics are missing, the score must be low
        assertTrue(item.getRecommendationScore() < 0.40,
                "Sparse profile with missing data must not receive artificial positive score points");
    }

    @Test
    @DisplayName("Verify grounded explainability: Reasons are derived strictly from matched attributes")
    void testExplainabilityReasonsAreGrounded() {
        when(citizenProfileRepository.findByUserId("user-65")).thenReturn(Optional.of(farmerMaharashtra));
        CitizenEligibilityEvaluationResponse eval = CitizenEligibilityEvaluationResponse.builder()
                .userId("user-65")
                .citizenState("Maharashtra")
                .summary(CitizenEligibilityEvaluationResponse.EvaluationSummary.builder().totalEvaluated(3).eligibleCount(1).build())
                .eligibleSchemes(List.of(
                        EligibilityEvaluationResult.builder().schemeCode("MH-AGRI-001").status(EligibilityStatus.ELIGIBLE).build()
                ))
                .build();
        when(eligibilityEvaluationService.evaluateCitizenAgainstAllSchemes("user-65")).thenReturn(eval);

        PersonalizedSchemeRecommendationResponse resp =
                recommendationService.getPersonalizedRecommendations("user-65", 0, 10);

        RankedSchemeItem item = resp.getRecommendations().get(0);
        assertNotNull(item.getReasons());
        assertFalse(item.getReasons().isEmpty());

        // Must include mandatory statutory notice
        boolean hasStatutoryNotice = item.getReasons().stream()
                .anyMatch(r -> r.contains("Statutory eligibility was confirmed by the EligibilityEngine"));
        assertTrue(hasStatutoryNotice);

        // Must include occupation match for Farmer
        boolean hasOccReason = item.getReasons().stream()
                .anyMatch(r -> r.contains("Farmer"));
        assertTrue(hasOccReason);

        // Must NOT claim state match for Tamil Nadu
        boolean hasWrongState = item.getReasons().stream()
                .anyMatch(r -> r.contains("Tamil Nadu"));
        assertFalse(hasWrongState);
    }
}
