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

/**
 * Phase 6C: Real-Data Eligibility Engine Validation Test Suite.
 * Validates deterministic tri-state logic, boundary conditions, AST rule structures,
 * and narrative text handling against real-world scheme constraints.
 */
class RealDataEligibilityValidationTest {

    private EligibilityEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EligibilityEngine();
    }

    // ── 1. Real Citizen Profiles & Central Scheme (PM-KISAN) ──────────────────

    @Test
    @DisplayName("TC-01: Real Farmer Profile against Central PM-KISAN Scheme -> ELIGIBLE")
    void testRealFarmer_PMKisan_Eligible() {
        // Real PM_KISAN conditions from catalog: Farmer = true, Income <= 200000, Age >= 18
        EligibilityCondition condFarmer = EligibilityCondition.builder()
                .field("FARMER").operator(RuleOperator.EQ).value("true").dataType("BOOLEAN").required(true).build();
        EligibilityCondition condIncome = EligibilityCondition.builder()
                .field("INCOME").operator(RuleOperator.LTE).value("200000").dataType("NUMBER").required(true).build();
        EligibilityCondition condAge = EligibilityCondition.builder()
                .field("AGE").operator(RuleOperator.GTE).value("18").dataType("NUMBER").required(true).build();

        Scheme pmKisan = Scheme.builder()
                .schemeCode("PM_KISAN_2026")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().logicalOperator("ALL").conditions(List.of(condFarmer, condIncome, condAge)).build())
                .build();

        // Real profile: User 65 (Aarav Sharma - Farmer, Age 34, Income 160000, Maharashtra)
        CitizenEligibilityProfile farmerProfile = CitizenEligibilityProfile.builder()
                .age(34)
                .annualIncome(160000.0)
                .occupation("Farmer")
                .state("Maharashtra")
                .attributes(Map.of("isFarmer", true))
                .build();

        EligibilityEvaluationResult result = engine.evaluate(farmerProfile, pmKisan);

        assertNotNull(result);
        assertEquals(EligibilityStatus.ELIGIBLE, result.getStatus());
        assertEquals(1.0, result.getConfidenceScore());
        assertTrue(result.getPassedConditions().stream().anyMatch(p -> p.contains("National Central Scheme")));
        assertTrue(result.getFailedConditions().isEmpty());
        assertTrue(result.getMissingAttributes().isEmpty());
    }

    // ── 2. Boundary Condition Tests (Age & Income) ────────────────────────────

    @Test
    @DisplayName("TC-02: Age Boundary Test - Exactly at Minimum Age (18) -> ELIGIBLE")
    void testAge_ExactMinimum_Eligible() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE").operator(RuleOperator.GTE).value("18").dataType("NUMBER").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("YOUTH-01").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(ageCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().age(18).build();
        assertEquals(EligibilityStatus.ELIGIBLE, engine.evaluate(citizen, scheme).getStatus());
    }

    @Test
    @DisplayName("TC-03: Age Boundary Test - Just Below Minimum Age (17) -> NOT_ELIGIBLE")
    void testAge_BelowMinimum_NotEligible() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE").operator(RuleOperator.GTE).value("18").dataType("NUMBER").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("YOUTH-01").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(ageCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().age(17).build();
        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
        assertFalse(res.getFailedConditions().isEmpty());
    }

    @Test
    @DisplayName("TC-04: Income Boundary Test - Exactly at Ceiling (200000) -> ELIGIBLE")
    void testIncome_ExactCeiling_Eligible() {
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("INCOME").operator(RuleOperator.LTE).value("200000").dataType("NUMBER").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("INC-01").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().annualIncome(200000.0).build();
        assertEquals(EligibilityStatus.ELIGIBLE, engine.evaluate(citizen, scheme).getStatus());
    }

    @Test
    @DisplayName("TC-05: Income Boundary Test - Just Above Ceiling (200001) -> NOT_ELIGIBLE")
    void testIncome_AboveCeiling_NotEligible() {
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("INCOME").operator(RuleOperator.LTE).value("200000").dataType("NUMBER").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("INC-01").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().annualIncome(200001.0).build();
        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
        assertTrue(res.getFailedConditions().stream().anyMatch(f -> f.contains("Requirement failed")));
    }

    // ── 3. Missing Data vs False Ineligibility (Three-State Logic) ────────────

    @Test
    @DisplayName("TC-06: Three-State Logic: Missing Income returns INSUFFICIENT_DATA, NEVER ELIGIBLE or NOT_ELIGIBLE")
    void testIncome_Missing_ReturnsInsufficientData() {
        EligibilityCondition incCond = EligibilityCondition.builder()
                .field("INCOME").operator(RuleOperator.LTE).value("200000").dataType("NUMBER").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("INC-01").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(incCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().annualIncome(null).build();
        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);

        assertEquals(EligibilityStatus.INSUFFICIENT_DATA, res.getStatus());
        assertTrue(res.getMissingAttributes().contains("INCOME"));
        assertTrue(res.getFailedConditions().isEmpty());
    }

    // ── 4. State Jurisdiction & Non-Matching Geography ─────────────────────────

    @Test
    @DisplayName("TC-07: State Scheme Jurisdiction Match -> ELIGIBLE")
    void testStateScheme_JurisdictionMatch_Eligible() {
        Scheme scheme = Scheme.builder()
                .schemeCode("TN-SCH-01")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Tamil Nadu")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
        assertTrue(res.getPassedConditions().get(0).contains("Tamil Nadu"));
    }

    @Test
    @DisplayName("TC-08: State Scheme Jurisdiction Mismatch (Karnataka in Tamil Nadu Scheme) -> NOT_ELIGIBLE")
    void testStateScheme_JurisdictionMismatch_NotEligible() {
        Scheme scheme = Scheme.builder()
                .schemeCode("TN-SCH-01")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Karnataka")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
        assertTrue(res.getFailedConditions().get(0).contains("Karnataka"));
    }

    // ── 5. Caste / Social Category (Zero Inference) ───────────────────────────

    @Test
    @DisplayName("TC-09: Social Category - Matching SC in SC/ST Scheme -> ELIGIBLE")
    void testCaste_Matching_Eligible() {
        EligibilityCondition casteCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY").operator(RuleOperator.IN).value("SC,ST").dataType("STRING").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("SC-ST-SCHEME").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(casteCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().socialCategory("SC").build();
        assertEquals(EligibilityStatus.ELIGIBLE, engine.evaluate(citizen, scheme).getStatus());
    }

    @Test
    @DisplayName("TC-10: Social Category - OBC Citizen in SC/ST Scheme -> NOT_ELIGIBLE")
    void testCaste_NonMatching_NotEligible() {
        EligibilityCondition casteCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY").operator(RuleOperator.IN).value("SC,ST").dataType("STRING").required(true).build();
        Scheme scheme = Scheme.builder().schemeCode("SC-ST-SCHEME").schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(casteCond)).build()).build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder().socialCategory("OBC").build();
        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, res.getStatus());
    }

    // ── 6. Complex Nested Boolean AST (ALL + ANY + NOT) ──────────────────────

    @Test
    @DisplayName("TC-11: Nested Boolean AST: ALL(State=TN, Age>=18, ANY(SC, ST)) -> ELIGIBLE")
    void testNestedAST_AllAndAny_Eligible() {
        EligibilityCondition ageCond = EligibilityCondition.builder()
                .field("AGE").operator(RuleOperator.GTE).value("18").dataType("NUMBER").required(true).build();
        EligibilityCondition scCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY").operator(RuleOperator.EQ).value("SC").dataType("STRING").required(true).build();
        EligibilityCondition stCond = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY").operator(RuleOperator.EQ).value("ST").dataType("STRING").required(true).build();

        RuleGroup anyGroup = RuleGroup.builder().logicalOperator("ANY").conditions(List.of(scCond, stCond)).build();
        RuleGroup rootGroup = RuleGroup.builder().logicalOperator("ALL").conditions(List.of(ageCond)).groups(List.of(anyGroup)).build();

        Scheme scheme = Scheme.builder()
                .schemeCode("COMPLEX-SCHEME")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Tamil Nadu")
                .eligibilityRules(rootGroup)
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Tamil Nadu")
                .age(22)
                .socialCategory("SC")
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
        assertTrue(res.getFailedConditions().isEmpty());
    }

    // ── 7. Narrative-Only Schemes (Zero Hallucination) ─────────────────────────

    @Test
    @DisplayName("TC-12: Narrative-Only Scheme: Returns INSUFFICIENT_DATA with narrativeEligibilityCriteria")
    void testNarrativeOnlyScheme_ReturnsInsufficientData() {
        Scheme scheme = Scheme.builder()
                .schemeCode("NARRATIVE-001")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().rawText("Applicants should belong to low income groups and have vocational training certificates.").build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .state("Maharashtra")
                .age(30)
                .annualIncome(150000.0)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.INSUFFICIENT_DATA, res.getStatus());
        assertTrue(res.getMissingAttributes().contains("narrativeEligibilityCriteria"));
        assertTrue(res.getExplanation().contains("narrative eligibility text"));
    }

    // ── 8. Disability (PwD) Evaluation ────────────────────────────────────────

    @Test
    @DisplayName("TC-13: Disability Scheme: Disability percentage >= 40% -> ELIGIBLE")
    void testDisability_PercentageMatch_Eligible() {
        EligibilityCondition pwdCond = EligibilityCondition.builder()
                .field("DISABILITY_STATUS").operator(RuleOperator.BOOLEAN_TRUE).dataType("BOOLEAN").required(true).build();

        Scheme scheme = Scheme.builder()
                .schemeCode("PWD-AID")
                .schemeLevel(SchemeLevel.CENTRAL)
                .eligibilityRules(RuleGroup.builder().conditions(List.of(pwdCond)).build())
                .build();

        CitizenEligibilityProfile citizen = CitizenEligibilityProfile.builder()
                .disabilityStatus(true)
                .build();

        EligibilityEvaluationResult res = engine.evaluate(citizen, scheme);
        assertEquals(EligibilityStatus.ELIGIBLE, res.getStatus());
    }
}
