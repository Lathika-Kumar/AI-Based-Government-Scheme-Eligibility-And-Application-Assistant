package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicEligibilityEngineTest {

    private EligibilityEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EligibilityEngine();
    }

    // ── 1. Geographical Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Central Scheme: Always passes geographic residency test nationwide")
    void testCentralScheme_GeographicPass() {
        Scheme scheme = Scheme.builder()
                .schemeCode("PM-KISAN")
                .schemeLevel(SchemeLevel.CENTRAL)
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Tamil Nadu")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertNotNull(res);
        assertFalse(res.getPassedConditions().isEmpty());
        assertTrue(res.getPassedConditions().get(0).contains("National Central Scheme"));
    }

    @Test
    @DisplayName("State Scheme: Matching state residency passes")
    void testStateScheme_MatchingState_Pass() {
        Scheme scheme = Scheme.builder()
                .schemeCode("TN-FREE-BUS")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Tamil Nadu")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertNotNull(res);
        assertTrue(res.getPassedConditions().stream().anyMatch(c -> c.contains("State residency verified")));
    }

    @Test
    @DisplayName("State Scheme: Non-matching state residency returns NOT_ELIGIBLE")
    void testStateScheme_NonMatchingState_NotEligible() {
        Scheme scheme = Scheme.builder()
                .schemeCode("TN-FREE-BUS")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Gujarat")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertNotNull(res);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
        assertTrue(res.getFailedConditions().stream().anyMatch(f -> f.contains("State residency requirement failed")));
    }

    @Test
    @DisplayName("State Scheme: Missing citizen state returns INSUFFICIENT_DATA")
    void testStateScheme_MissingState_InsufficientData() {
        Scheme scheme = Scheme.builder()
                .schemeCode("TN-FREE-BUS")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state(null)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertNotNull(res);
        assertEquals(EligibilityStatus.INSUFFICIENT_DATA, res.getStatus());
        assertTrue(res.getMissingAttributes().contains("state"));
    }

    // ── 2. Age Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Age: Satisfies minimum threshold (age >= 18)")
    void testAge_MinThreshold_Pass() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("YOUTH-SKILL")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(ageCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .age(25)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Age: Below minimum threshold returns NOT_ELIGIBLE")
    void testAge_BelowMin_NotEligible() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("YOUTH-SKILL")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(ageCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .age(16)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Age: Missing age returns INSUFFICIENT_DATA")
    void testAge_Missing_InsufficientData() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("YOUTH-SKILL")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(ageCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .age(null)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.INSUFFICIENT_DATA, res.getStatus());
        assertTrue(res.getMissingAttributes().contains("AGE"));
    }

    // ── 3. Income Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Income: Below and equal threshold passes")
    void testIncome_Threshold_Pass() {
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("ANNUAL_INCOME")
                .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                .value("250000")
                .dataType("NUMBER")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("EWS-AID")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .annualIncome(250000.0)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Income: Above threshold returns NOT_ELIGIBLE")
    void testIncome_AboveThreshold_NotEligible() {
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("ANNUAL_INCOME")
                .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                .value("250000")
                .dataType("NUMBER")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("EWS-AID")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .annualIncome(300000.0)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
    }

    // ── 4. Social Category / Caste Tests ──────────────────────────────────────

    @Test
    @DisplayName("Caste: Matching SC in SC/ST rule passes")
    void testCaste_Matching_Pass() {
        EligibilityCondition casteCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY")
                .operator(RuleOperator.IN)
                .value("SC,ST")
                .dataType("STRING")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("POST-MATRIC-SC")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(casteCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .socialCategory("SC")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Caste: Non-matching General in SC/ST rule returns NOT_ELIGIBLE")
    void testCaste_NonMatching_NotEligible() {
        EligibilityCondition casteCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY")
                .operator(RuleOperator.IN)
                .value("SC,ST")
                .dataType("STRING")
                .required(true)
                .build();

        Scheme scheme = Scheme.builder()
                .schemeCode("POST-MATRIC-SC")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(casteCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .socialCategory("General")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
    }

    // ── 5. Boolean Combination Tests (AND, OR, NOT) ───────────────────────────

    @Test
    @DisplayName("Boolean AND: All conditions must pass for ELIGIBLE")
    void testBoolean_AND_Pass() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE").operator(RuleOperator.GREATER_THAN_OR_EQUAL).value("18").dataType("NUMBER").required(true).build();
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("ANNUAL_INCOME").operator(RuleOperator.LESS_THAN_OR_EQUAL).value("300000").dataType("NUMBER").required(true).build();

        Scheme scheme = Scheme.builder()
                .schemeCode("COMBINED-AID")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ALL")
                        .conditions(List.of(ageCond, incCond))
                        .build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .age(22)
                .annualIncome(200000.0)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Boolean OR: Any matching condition satisfies group")
    void testBoolean_OR_Pass() {
        EligibilityCondition farmerCond = EligibilityCondition.builder()
                .field("IS_FARMER").operator(RuleOperator.EQUALS).value("true").dataType("BOOLEAN").required(true).build();
        EligibilityCondition artisanCond = EligibilityCondition.builder()
                .field("OCCUPATION").operator(RuleOperator.EQUALS).value("Artisan").dataType("STRING").required(true).build();

        Scheme scheme = Scheme.builder()
                .schemeCode("RURAL-LIVELIHOOD")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder()
                        .logicalOperator("ANY")
                        .conditions(List.of(farmerCond, artisanCond))
                        .build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .occupation("Artisan")
                .attributes(Map.of("isFarmer", false))
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }

    @Test
    @DisplayName("Verified Attribute Tracking: Provenance is preserved in result")
    void testVerifiedAttribute_Tracking() {
        VerifiedAttribute<Double> incAttr = VerifiedAttribute.verified(
                150000.0, "DOCUMENT_VERIFIED", "doc-1", "INCOME_CERTIFICATE", 0.99
        );

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("verifiedAttributes", Map.of("ANNUAL_INCOME", incAttr));

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .annualIncome(150000.0)
                .attributes(attrs)
                .build();

        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("ANNUAL_INCOME").operator(RuleOperator.LESS_THAN_OR_EQUAL).value("200000").dataType("NUMBER").required(true).build();

        Scheme scheme = Scheme.builder()
                .schemeCode("INCOME-SCHEME")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build())
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
        assertTrue(res.getVerifiedAttributesUsed().contains("ANNUAL_INCOME"));
    }
}
