package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeRecommendationTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRecommendationService schemeRecommendationService;

    @BeforeEach
    public void setUp() {
        schemeRepository.deleteAll();

        // 1. Fully Eligible Scheme for Age 21
        Scheme s1 = Scheme.builder()
                .schemeCode("REC-01")
                .slug("eligible-youth-scheme")
                .title(MultilingualText.builder().english("Youth Development Scheme").build())
                .shortDescription(MultilingualText.builder().english("Desc 1").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("18")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        // 2. Near Match Scheme (Fails 1 out of 3, pass rate is 66.7% >= 50%)
        Scheme s2 = Scheme.builder()
                .schemeCode("REC-02")
                .slug("farmers-subsidy-scheme")
                .title(MultilingualText.builder().english("Farmers Subsidy").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("OCCUPATION")
                                        .operator(RuleOperator.EQUALS)
                                        .value("FARMER")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("STATE")
                                        .operator(RuleOperator.EQUALS)
                                        .value("TAMIL_NADU")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("18")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        // 3. Excluded Scheme (Fails 2 out of 3, pass rate is 33.3% < 50%)
        Scheme s3 = Scheme.builder()
                .schemeCode("REC-03")
                .slug("general-insurance-scheme")
                .title(MultilingualText.builder().english("General Insurance").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("OCCUPATION")
                                        .operator(RuleOperator.EQUALS)
                                        .value("BUSINESS")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("STATE")
                                        .operator(RuleOperator.EQUALS)
                                        .value("PUNJAB")
                                        .dataType("STRING")
                                        .required(true)
                                        .build(),
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("18")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        // 4. Indeterminate (Missing Required Income Field)
        Scheme s4 = Scheme.builder()
                .schemeCode("REC-04")
                .slug("low-income-support")
                .title(MultilingualText.builder().english("Low Income Support").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("ANNUAL_INCOME")
                                        .operator(RuleOperator.LESS_THAN)
                                        .value("100000")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        // 5. Legacy Free Text (Resolves to Indeterminate)
        Scheme s5 = Scheme.builder()
                .schemeCode("REC-05")
                .slug("legacy-scholars")
                .title(MultilingualText.builder().english("Legacy Scholars").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("LEGACY_FREE_TEXT")
                                        .description(MultilingualText.builder().english("Must hold merit index").build())
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        // 6. Draft Scheme (Privileged visibility checks)
        Scheme s6 = Scheme.builder()
                .schemeCode("REC-06")
                .slug("draft-welfare-scheme")
                .title(MultilingualText.builder().english("Draft Welfare").build())
                .status(SchemeStatus.DRAFT)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(
                                EligibilityCondition.builder()
                                        .field("AGE")
                                        .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                                        .value("18")
                                        .dataType("NUMBER")
                                        .required(true)
                                        .build()
                        ))
                        .build())
                .build();

        schemeRepository.saveAll(List.of(s1, s2, s3, s4, s5, s6));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEligibleProfileScoreAndCategory() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(21)
                .build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);
        
        // REC-01 should be ELIGIBLE, score 100
        RecommendationResponseItem eligibleItem = res.getRecommendations().stream()
                .filter(r -> r.getSchemeCode().equals("REC-01"))
                .findFirst()
                .orElseThrow();

        assertEquals(RecommendationCategory.ELIGIBLE, eligibleItem.getRecommendationCategory());
        assertEquals(100.0, eligibleItem.getMatchScore());
        assertTrue(eligibleItem.getReasons().contains("Meets all required eligibility conditions."));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testNearMatchProfileScoreAndCategory() {
        // Fails OCCUPATION (is STUDENT, not FARMER) but matches STATE and AGE -> pass rate is 2/3 = 66.7%
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(21)
                .occupation("STUDENT")
                .state("TAMIL_NADU")
                .build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);

        RecommendationResponseItem nearMatchItem = res.getRecommendations().stream()
                .filter(r -> r.getSchemeCode().equals("REC-02"))
                .findFirst()
                .orElseThrow();

        assertEquals(RecommendationCategory.NEAR_MATCH, nearMatchItem.getRecommendationCategory());
        // matchScore = passed / total = 2 / 3 * 100 = 66.67
        assertEquals(66.66666666666666, nearMatchItem.getMatchScore());
        assertTrue(nearMatchItem.getReasons().get(0).startsWith("Meets 2 of 3 evaluated"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testNotEligibleExcluded() {
        // Fails OCCUPATION (STUDENT vs BUSINESS) and STATE (TAMIL_NADU vs PUNJAB) -> pass rate is 1/3 = 33.3% < 50%
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(21)
                .occupation("STUDENT")
                .state("TAMIL_NADU")
                .build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);

        boolean containsExcluded = res.getRecommendations().stream()
                .anyMatch(r -> r.getSchemeCode().equals("REC-03"));

        assertFalse(containsExcluded);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testIndeterminateProfileMissingInfo() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(21)
                // Omit income
                .build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);

        RecommendationResponseItem item = res.getRecommendations().stream()
                .filter(r -> r.getSchemeCode().equals("REC-04"))
                .findFirst()
                .orElseThrow();

        assertEquals(RecommendationCategory.INDETERMINATE, item.getRecommendationCategory());
        assertTrue(item.getReasons().contains("Missing required information: ANNUAL_INCOME"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testLegacyFreeTextIndeterminate() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);

        RecommendationResponseItem item = res.getRecommendations().stream()
                .filter(r -> r.getSchemeCode().equals("REC-05"))
                .findFirst()
                .orElseThrow();

        assertEquals(RecommendationCategory.INDETERMINATE, item.getRecommendationCategory());
        assertTrue(item.getReasons().contains("Requires manual verification: Must hold merit index"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testDeterministicRankingOrder() {
        // Setup profile:
        // - REC-01 matches completely (ELIGIBLE, Score 100)
        // - REC-02 fails OCCUPATION but matches STATE and AGE (NEAR_MATCH, Score 66.7)
        // - REC-04 is missing ANNUAL_INCOME (INDETERMINATE, Score 0)
        // - REC-05 has legacy text (INDETERMINATE, Score 0)
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(21)
                .occupation("STUDENT")
                .state("TAMIL_NADU")
                .build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);
        List<RecommendationResponseItem> items = res.getRecommendations();

        // Should be:
        // 0: REC-01 (ELIGIBLE)
        // 1: REC-02 (NEAR_MATCH)
        // 2: REC-04 (INDETERMINATE, tie-breaker order)
        // 3: REC-05 (INDETERMINATE)
        assertEquals("REC-01", items.get(0).getSchemeCode());
        assertEquals("REC-02", items.get(1).getSchemeCode());
        assertEquals("REC-04", items.get(2).getSchemeCode());
        assertEquals("REC-05", items.get(3).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testPaginationAndLimits() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 2);
        assertEquals(2, res.getRecommendations().size());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidPaginationParameters() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        assertThrows(IllegalArgumentException.class, () ->
                schemeRecommendationService.getRecommendations(profile, null, -1, 10)
        );
        assertThrows(IllegalArgumentException.class, () ->
                schemeRecommendationService.getRecommendations(profile, null, 0, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                schemeRecommendationService.getRecommendations(profile, null, 0, 51)
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserSeesActiveOnly() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, null, 0, 10);
        
        // Assert total schemes evaluated matches 5 active ones (excluding draft REC-06)
        assertEquals(5, res.getTotalSchemesEvaluated());
        boolean containsDraft = res.getRecommendations().stream()
                .anyMatch(r -> r.getSchemeCode().equals("REC-06"));
        assertFalse(containsDraft);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserCannotAccessDraftRecommendations() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        assertThrows(SecurityException.class, () ->
                schemeRecommendationService.getRecommendations(profile, "DRAFT", 0, 10)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminCanAccessDraftRecommendations() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        PersonalizedRecommendationResponse res = schemeRecommendationService.getRecommendations(profile, "DRAFT", 0, 10);
        assertEquals(1, res.getTotalSchemesEvaluated());
        assertEquals("REC-06", res.getRecommendations().get(0).getSchemeCode());
    }
}
