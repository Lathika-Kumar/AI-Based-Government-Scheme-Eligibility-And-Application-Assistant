package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EligibilityEngineTest {

    private final EligibilityEngine engine = new EligibilityEngine();

    @Test
    public void testEvaluateScheme_NoRules_ReturnsEligible() {
        Scheme scheme = Scheme.builder().id("sch-no-rules").schemeCode("SCH-000").build();
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().build();

        EligibilityEvaluationResponse response = engine.evaluateScheme(scheme, profile);
        assertEquals(EvaluationStatus.ELIGIBLE, response.getStatus());
        assertTrue(response.getMatchedConditions().isEmpty());
    }

    @Test
    public void testOperators_Equals_And_NotEquals() {
        EligibilityCondition condEquals = EligibilityCondition.builder()
                .field("occupation")
                .operator(RuleOperator.EQUALS)
                .value("FARMER")
                .dataType("STRING")
                .build();

        EligibilityCondition condNotEquals = EligibilityCondition.builder()
                .field("gender")
                .operator(RuleOperator.NOT_EQUALS)
                .value("MALE")
                .dataType("STRING")
                .build();

        RuleGroup group = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(condEquals, condNotEquals))
                .build();

        Scheme scheme = Scheme.builder().eligibilityRules(group).build();

        // Match case
        CitizenEligibilityProfile p1 = CitizenEligibilityProfile.builder()
                .occupation("FARMER")
                .gender("FEMALE")
                .build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(scheme, p1).getStatus());

        // Fail case (equals fails)
        CitizenEligibilityProfile p2 = CitizenEligibilityProfile.builder()
                .occupation("STUDENT")
                .gender("FEMALE")
                .build();
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(scheme, p2).getStatus());

        // Fail case (notEquals fails)
        CitizenEligibilityProfile p3 = CitizenEligibilityProfile.builder()
                .occupation("FARMER")
                .gender("MALE")
                .build();
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(scheme, p3).getStatus());
    }

    @Test
    public void testOperators_NumericComparisons() {
        EligibilityCondition gt = EligibilityCondition.builder()
                .field("age")
                .operator(RuleOperator.GREATER_THAN)
                .value("18")
                .dataType("NUMBER")
                .build();

        EligibilityCondition gte = EligibilityCondition.builder()
                .field("age")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .build();

        EligibilityCondition lt = EligibilityCondition.builder()
                .field("annualIncome")
                .operator(RuleOperator.LESS_THAN)
                .value("300000")
                .dataType("NUMBER")
                .build();

        EligibilityCondition lte = EligibilityCondition.builder()
                .field("annualIncome")
                .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                .value("300000")
                .dataType("NUMBER")
                .build();

        // GREATER_THAN
        Scheme sGt = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(gt)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sGt, CitizenEligibilityProfile.builder().age(19).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sGt, CitizenEligibilityProfile.builder().age(18).build()).getStatus());

        // GREATER_THAN_OR_EQUAL
        Scheme sGte = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(gte)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sGte, CitizenEligibilityProfile.builder().age(18).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sGte, CitizenEligibilityProfile.builder().age(17).build()).getStatus());

        // LESS_THAN
        Scheme sLt = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(lt)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sLt, CitizenEligibilityProfile.builder().annualIncome(299999.0).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sLt, CitizenEligibilityProfile.builder().annualIncome(300000.0).build()).getStatus());

        // LESS_THAN_OR_EQUAL
        Scheme sLte = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(lte)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sLte, CitizenEligibilityProfile.builder().annualIncome(300000.0).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sLte, CitizenEligibilityProfile.builder().annualIncome(300001.0).build()).getStatus());
    }

    @Test
    public void testOperators_Between_In_NotIn() {
        EligibilityCondition between = EligibilityCondition.builder()
                .field("age")
                .operator(RuleOperator.BETWEEN)
                .value("18,35")
                .dataType("NUMBER")
                .build();

        EligibilityCondition in = EligibilityCondition.builder()
                .field("state")
                .operator(RuleOperator.IN)
                .value("TAMIL_NADU, KERALA")
                .dataType("STRING")
                .build();

        EligibilityCondition notIn = EligibilityCondition.builder()
                .field("socialCategory")
                .operator(RuleOperator.NOT_IN)
                .value("GENERAL")
                .dataType("STRING")
                .build();

        // BETWEEN
        Scheme sBetween = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(between)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sBetween, CitizenEligibilityProfile.builder().age(18).build()).getStatus());
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sBetween, CitizenEligibilityProfile.builder().age(25).build()).getStatus());
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sBetween, CitizenEligibilityProfile.builder().age(35).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sBetween, CitizenEligibilityProfile.builder().age(36).build()).getStatus());

        // IN
        Scheme sIn = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(in)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sIn, CitizenEligibilityProfile.builder().state("KERALA").build()).getStatus());
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sIn, CitizenEligibilityProfile.builder().state("tamil_nadu").build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sIn, CitizenEligibilityProfile.builder().state("KARNATAKA").build()).getStatus());

        // NOT_IN
        Scheme sNotIn = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(notIn)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sNotIn, CitizenEligibilityProfile.builder().socialCategory("OBC").build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sNotIn, CitizenEligibilityProfile.builder().socialCategory("GENERAL").build()).getStatus());
    }

    @Test
    public void testOperators_Contains_BooleanTrue_BooleanFalse() {
        EligibilityCondition contains = EligibilityCondition.builder()
                .field("occupation")
                .operator(RuleOperator.CONTAINS)
                .value("FARM")
                .dataType("STRING")
                .build();

        EligibilityCondition isTrue = EligibilityCondition.builder()
                .field("disabilityStatus")
                .operator(RuleOperator.BOOLEAN_TRUE)
                .dataType("BOOLEAN")
                .build();

        EligibilityCondition isFalse = EligibilityCondition.builder()
                .field("disabilityStatus")
                .operator(RuleOperator.BOOLEAN_FALSE)
                .dataType("BOOLEAN")
                .build();

        // CONTAINS
        Scheme sContains = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(contains)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sContains, CitizenEligibilityProfile.builder().occupation("FARMER").build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sContains, CitizenEligibilityProfile.builder().occupation("STUDENT").build()).getStatus());

        // BOOLEAN_TRUE
        Scheme sTrue = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(isTrue)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sTrue, CitizenEligibilityProfile.builder().disabilityStatus(true).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sTrue, CitizenEligibilityProfile.builder().disabilityStatus(false).build()).getStatus());

        // BOOLEAN_FALSE
        Scheme sFalse = Scheme.builder().eligibilityRules(RuleGroup.builder().conditions(List.of(isFalse)).build()).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sFalse, CitizenEligibilityProfile.builder().disabilityStatus(false).build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sFalse, CitizenEligibilityProfile.builder().disabilityStatus(true).build()).getStatus());
    }

    @Test
    public void testGroups_ALL_And_ANY() {
        EligibilityCondition c1 = EligibilityCondition.builder().field("age").operator(RuleOperator.GREATER_THAN_OR_EQUAL).value("18").dataType("NUMBER").build();
        EligibilityCondition c2 = EligibilityCondition.builder().field("occupation").operator(RuleOperator.EQUALS).value("STUDENT").dataType("STRING").build();
        EligibilityCondition c3 = EligibilityCondition.builder().field("occupation").operator(RuleOperator.EQUALS).value("FARMER").dataType("STRING").build();

        // ALL (AND): Both c1 and c2 must be satisfied
        RuleGroup gAll = RuleGroup.builder().logicalOperator("ALL").conditions(List.of(c1, c2)).build();
        Scheme sAll = Scheme.builder().eligibilityRules(gAll).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sAll, CitizenEligibilityProfile.builder().age(20).occupation("STUDENT").build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sAll, CitizenEligibilityProfile.builder().age(17).occupation("STUDENT").build()).getStatus());

        // ANY (OR): Either c2 or c3 must be satisfied
        RuleGroup gAny = RuleGroup.builder().logicalOperator("ANY").conditions(List.of(c2, c3)).build();
        Scheme sAny = Scheme.builder().eligibilityRules(gAny).build();
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sAny, CitizenEligibilityProfile.builder().occupation("STUDENT").build()).getStatus());
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(sAny, CitizenEligibilityProfile.builder().occupation("FARMER").build()).getStatus());
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(sAny, CitizenEligibilityProfile.builder().occupation("TEACHER").build()).getStatus());
    }

    @Test
    public void testGroups_NestedGroups() {
        EligibilityCondition age = EligibilityCondition.builder().field("age").operator(RuleOperator.GREATER_THAN_OR_EQUAL).value("18").dataType("NUMBER").build();
        EligibilityCondition student = EligibilityCondition.builder().field("occupation").operator(RuleOperator.EQUALS).value("STUDENT").dataType("STRING").build();
        EligibilityCondition farmer = EligibilityCondition.builder().field("occupation").operator(RuleOperator.EQUALS).value("FARMER").dataType("STRING").build();

        // ALL
        //  ├── age >= 18
        //  └── ANY
        //       ├── occupation == STUDENT
        //       └── occupation == FARMER
        RuleGroup innerGroup = RuleGroup.builder().logicalOperator("ANY").conditions(List.of(student, farmer)).build();
        RuleGroup rootGroup = RuleGroup.builder().logicalOperator("ALL").conditions(List.of(age)).groups(List.of(innerGroup)).build();
        Scheme scheme = Scheme.builder().eligibilityRules(rootGroup).build();

        // Case 1: age=20, occupation=STUDENT -> ELIGIBLE
        assertEquals(EvaluationStatus.ELIGIBLE, engine.evaluateScheme(scheme, CitizenEligibilityProfile.builder().age(20).occupation("STUDENT").build()).getStatus());

        // Case 2: age=17, occupation=STUDENT -> NOT_ELIGIBLE (root condition fails)
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(scheme, CitizenEligibilityProfile.builder().age(17).occupation("STUDENT").build()).getStatus());

        // Case 3: age=20, occupation=TEACHER -> NOT_ELIGIBLE (inner group fails)
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, engine.evaluateScheme(scheme, CitizenEligibilityProfile.builder().age(20).occupation("TEACHER").build()).getStatus());
    }

    @Test
    public void testMissingData_ReturnsIndeterminate() {
        EligibilityCondition cond = EligibilityCondition.builder()
                .field("disabilityStatus")
                .operator(RuleOperator.BOOLEAN_TRUE)
                .dataType("BOOLEAN")
                .required(true)
                .build();

        RuleGroup group = RuleGroup.builder().conditions(List.of(cond)).build();
        Scheme scheme = Scheme.builder().eligibilityRules(group).build();

        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().build(); // disabilityStatus is null

        EligibilityEvaluationResponse res = engine.evaluateScheme(scheme, profile);
        assertEquals(EvaluationStatus.INDETERMINATE, res.getStatus());
        assertTrue(res.getMissingInformation().contains("disabilityStatus"));
    }

    @Test
    public void testLegacyFreeText_ReturnsIndeterminate() {
        EligibilityCondition cond = EligibilityCondition.builder()
                .field("LEGACY_FREE_TEXT")
                .value("Minimum 60% in class 12")
                .question(MultilingualText.builder().english("Minimum 60% in class 12").build())
                .build();

        RuleGroup group = RuleGroup.builder().conditions(List.of(cond)).build();
        Scheme scheme = Scheme.builder().eligibilityRules(group).build();

        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().build();

        EligibilityEvaluationResponse res = engine.evaluateScheme(scheme, profile);
        assertEquals(EvaluationStatus.INDETERMINATE, res.getStatus());
        assertTrue(res.getMissingInformation().get(0).contains("requires manual verification"));
    }

    @Test
    public void testInvalidDataType_ReturnsNotEligibleExplanation() {
        EligibilityCondition cond = EligibilityCondition.builder()
                .field("age")
                .operator(RuleOperator.GREATER_THAN)
                .value("INVALID_NUMBER")
                .dataType("NUMBER")
                .build();

        RuleGroup group = RuleGroup.builder().conditions(List.of(cond)).build();
        Scheme scheme = Scheme.builder().eligibilityRules(group).build();

        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(25).build();

        EligibilityEvaluationResponse res = engine.evaluateScheme(scheme, profile);
        assertEquals(EvaluationStatus.NOT_ELIGIBLE, res.getStatus());
        assertTrue(res.getFailedConditions().get(0).contains("Controlled evaluation error"));
    }
}
