package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse.ConditionEvaluationDetail;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * Deterministic Citizen Eligibility Engine (Phase 6B).
 *
 * Core Principles:
 *   1. 100% Deterministic — Evaluates boolean AST rules against verified citizen profile attributes.
 *   2. Strict Tri-State Logic — Produces ELIGIBLE, NOT_ELIGIBLE, or INSUFFICIENT_DATA.
 *   3. Zero Guessing / Zero Hallucination — Never assumes missing fields or fabricates rules.
 *   4. Clean AST Branching — Supports nested ALL (AND), ANY (OR), and NOT operators.
 *   5. Clear Provenance — Records which verified attributes were utilized.
 */
@Service
@Slf4j
public class EligibilityEngine implements EligibilityEngineContract {

    @Override
    public EligibilityEvaluationResult evaluate(CitizenEligibilityProfile profile, Scheme scheme) {
        String schemeCode = scheme.getSchemeCode() != null ? scheme.getSchemeCode() : "UNKNOWN";
        String schemeTitle = scheme.getTitle() != null ? scheme.getTitle().getEnglish() : schemeCode;
        String schemeLevel = scheme.getSchemeLevel() != null ? scheme.getSchemeLevel().name() : "CENTRAL";
        String catCode = scheme.getCategory() != null ? scheme.getCategory().getCode() : "GENERAL";
        String catName = scheme.getCategory() != null ? scheme.getCategory().getName() : catCode;

        List<String> passedConditions = new ArrayList<>();
        List<String> failedConditions = new ArrayList<>();
        List<String> missingAttributes = new ArrayList<>();
        List<String> verifiedAttributesUsed = new ArrayList<>();

        Map<String, VerifiedAttribute<?>> verifiedMap = extractVerifiedAttributesMap(profile);

        // ── 1. Geographical Eligibility Evaluation ──────────────────────────────
        boolean geoPassed = evaluateGeography(scheme, profile, passedConditions, failedConditions, missingAttributes, verifiedAttributesUsed, verifiedMap);

        if (!geoPassed && !failedConditions.isEmpty()) {
            return EligibilityEvaluationResult.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(schemeCode)
                    .slug(scheme.getSlug())
                    .schemeTitle(schemeTitle)
                    .schemeLevel(schemeLevel)
                    .categoryCode(catCode)
                    .categoryName(catName)
                    .status(EligibilityStatus.NOT_ELIGIBLE)
                    .confidenceScore(1.0)
                    .passedConditions(passedConditions)
                    .failedConditions(failedConditions)
                    .missingAttributes(missingAttributes)
                    .verifiedAttributesUsed(verifiedAttributesUsed)
                    .explanation("Failed state/geographic jurisdiction requirement.")
                    .build();
        }

        // ── 2. Structured Rules Evaluation ──────────────────────────────────────
        RuleGroup rules = scheme.getEligibilityRules();
        boolean hasStructuredConditions = rules != null && (
                (rules.getConditions() != null && !rules.getConditions().isEmpty()) ||
                (rules.getGroups() != null && !rules.getGroups().isEmpty())
        );

        if (hasStructuredConditions) {
            List<ConditionEvaluationDetail> details = new ArrayList<>();
            EvaluationStatus ruleStatus = evaluateRuleGroup(rules, profile, passedConditions, failedConditions, missingAttributes, details);

            for (ConditionEvaluationDetail d : details) {
                if (d.getField() != null && verifiedMap.containsKey(d.getField())) {
                    if (!verifiedAttributesUsed.contains(d.getField())) {
                        verifiedAttributesUsed.add(d.getField());
                    }
                }
            }

            EligibilityStatus finalStatus;
            String explanation;

            if (ruleStatus == EvaluationStatus.NOT_ELIGIBLE) {
                finalStatus = EligibilityStatus.NOT_ELIGIBLE;
                explanation = "One or more mandatory eligibility conditions were not satisfied.";
            } else if (ruleStatus == EvaluationStatus.INDETERMINATE || !missingAttributes.isEmpty()) {
                finalStatus = EligibilityStatus.INSUFFICIENT_DATA;
                explanation = "Additional citizen demographic or document information is required for complete eligibility evaluation.";
            } else {
                finalStatus = EligibilityStatus.ELIGIBLE;
                explanation = "All structured eligibility criteria and geographic requirements were successfully verified.";
            }

            return EligibilityEvaluationResult.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(schemeCode)
                    .slug(scheme.getSlug())
                    .schemeTitle(schemeTitle)
                    .schemeLevel(schemeLevel)
                    .categoryCode(catCode)
                    .categoryName(catName)
                    .status(finalStatus)
                    .confidenceScore(finalStatus == EligibilityStatus.ELIGIBLE ? 1.0 : (finalStatus == EligibilityStatus.NOT_ELIGIBLE ? 1.0 : 0.5))
                    .passedConditions(passedConditions)
                    .failedConditions(failedConditions)
                    .missingAttributes(missingAttributes)
                    .verifiedAttributesUsed(verifiedAttributesUsed)
                    .explanation(explanation)
                    .build();
        }

        // ── 3. Narrative Text / No Rules Evaluation ─────────────────────────────
        if (!missingAttributes.isEmpty()) {
            return EligibilityEvaluationResult.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(schemeCode)
                    .slug(scheme.getSlug())
                    .schemeTitle(schemeTitle)
                    .schemeLevel(schemeLevel)
                    .categoryCode(catCode)
                    .categoryName(catName)
                    .status(EligibilityStatus.INSUFFICIENT_DATA)
                    .confidenceScore(0.5)
                    .passedConditions(passedConditions)
                    .failedConditions(failedConditions)
                    .missingAttributes(missingAttributes)
                    .verifiedAttributesUsed(verifiedAttributesUsed)
                    .explanation("Missing citizen location data for jurisdiction evaluation.")
                    .build();
        }

        if (rules != null && rules.getRawText() != null && !rules.getRawText().isBlank()) {
            missingAttributes.add("narrativeEligibilityCriteria");
            return EligibilityEvaluationResult.builder()
                    .schemeId(scheme.getId())
                    .schemeCode(schemeCode)
                    .slug(scheme.getSlug())
                    .schemeTitle(schemeTitle)
                    .schemeLevel(schemeLevel)
                    .categoryCode(catCode)
                    .categoryName(catName)
                    .status(EligibilityStatus.INSUFFICIENT_DATA)
                    .confidenceScore(0.6)
                    .passedConditions(passedConditions)
                    .failedConditions(failedConditions)
                    .missingAttributes(missingAttributes)
                    .verifiedAttributesUsed(verifiedAttributesUsed)
                    .explanation("Geographic applicability verified. Scheme contains narrative eligibility text requiring individual criteria review.")
                    .build();
        }

        // Default: If completely open scheme with no rules and no raw text
        return EligibilityEvaluationResult.builder()
                .schemeId(scheme.getId())
                .schemeCode(schemeCode)
                .slug(scheme.getSlug())
                .schemeTitle(schemeTitle)
                .schemeLevel(schemeLevel)
                .categoryCode(catCode)
                .categoryName(catName)
                .status(EligibilityStatus.ELIGIBLE)
                .confidenceScore(1.0)
                .passedConditions(passedConditions)
                .failedConditions(failedConditions)
                .missingAttributes(missingAttributes)
                .verifiedAttributesUsed(verifiedAttributesUsed)
                .explanation("Open public scheme with no restrictive eligibility barriers.")
                .build();
    }

    private boolean evaluateGeography(
            Scheme scheme,
            CitizenEligibilityProfile profile,
            List<String> passed,
            List<String> failed,
            List<String> missing,
            List<String> verifiedAttrs,
            Map<String, VerifiedAttribute<?>> verifiedMap) {

        SchemeLevel level = scheme.getSchemeLevel();
        String stateOrUt = scheme.getStateOrUt();

        if (level == SchemeLevel.CENTRAL || (stateOrUt == null || stateOrUt.isBlank() || "ALL".equalsIgnoreCase(stateOrUt))) {
            passed.add("National Central Scheme: Applicable across all States and Union Territories.");
            return true;
        }

        String citizenState = profile.getState();
        if (citizenState == null || citizenState.isBlank()) {
            missing.add("state");
            return false;
        }

        if (verifiedMap.containsKey("state")) {
            verifiedAttrs.add("state");
        }

        if (stateOrUt.trim().equalsIgnoreCase(citizenState.trim())) {
            passed.add("State residency verified: " + citizenState + " matches scheme jurisdiction (" + stateOrUt + ").");
            return true;
        } else {
            failed.add("State residency requirement failed: Scheme is restricted to residents of " + stateOrUt + ", but citizen resides in " + citizenState + ".");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, VerifiedAttribute<?>> extractVerifiedAttributesMap(CitizenEligibilityProfile profile) {
        if (profile != null && profile.getAttributes() != null && profile.getAttributes().containsKey("verifiedAttributes")) {
            Object obj = profile.getAttributes().get("verifiedAttributes");
            if (obj instanceof Map) {
                return (Map<String, VerifiedAttribute<?>>) obj;
            }
        }
        return Collections.emptyMap();
    }

    // ── Legacy Compatibility Method ──────────────────────────────────────────

    public EligibilityEvaluationResponse evaluateScheme(Scheme scheme, CitizenEligibilityProfile profile) {
        log.info("Evaluating eligibility for schemeCode={} / slug={}", scheme.getSchemeCode(), scheme.getSlug());

        List<String> matchedConditions = new ArrayList<>();
        List<String> failedConditions = new ArrayList<>();
        List<String> missingInformation = new ArrayList<>();
        List<ConditionEvaluationDetail> details = new ArrayList<>();

        EvaluationStatus finalStatus = EvaluationStatus.ELIGIBLE;

        if (scheme.getEligibilityRules() != null) {
            finalStatus = evaluateRuleGroup(
                    scheme.getEligibilityRules(),
                    profile,
                    matchedConditions,
                    failedConditions,
                    missingInformation,
                    details
            );
        }

        return EligibilityEvaluationResponse.builder()
                .schemeId(scheme.getId())
                .schemeCode(scheme.getSchemeCode())
                .status(finalStatus)
                .matchedConditions(matchedConditions)
                .failedConditions(failedConditions)
                .missingInformation(missingInformation)
                .details(details)
                .build();
    }

    // ── Rule Group & Condition Tree Evaluator ─────────────────────────────────

    private EvaluationStatus evaluateRuleGroup(
            RuleGroup group,
            CitizenEligibilityProfile profile,
            List<String> matched,
            List<String> failed,
            List<String> missing,
            List<ConditionEvaluationDetail> details) {

        String logicalOperator = group.getLogicalOperator() != null ? group.getLogicalOperator().toUpperCase() : "ALL";

        List<EvaluationStatus> childStatuses = new ArrayList<>();
        List<List<String>> childMatched = new ArrayList<>();
        List<List<String>> childFailed = new ArrayList<>();
        List<List<String>> childMissing = new ArrayList<>();

        if (group.getConditions() != null) {
            for (EligibilityCondition cond : group.getConditions()) {
                List<String> localMatched = new ArrayList<>();
                List<String> localFailed = new ArrayList<>();
                List<String> localMissing = new ArrayList<>();

                EvaluationStatus status = evaluateCondition(cond, profile, localMatched, localFailed, localMissing, details);
                childStatuses.add(status);
                childMatched.add(localMatched);
                childFailed.add(localFailed);
                childMissing.add(localMissing);
            }
        }

        if (group.getGroups() != null) {
            for (RuleGroup subGroup : group.getGroups()) {
                List<String> localMatched = new ArrayList<>();
                List<String> localFailed = new ArrayList<>();
                List<String> localMissing = new ArrayList<>();

                EvaluationStatus status = evaluateRuleGroup(subGroup, profile, localMatched, localFailed, localMissing, details);
                childStatuses.add(status);
                childMatched.add(localMatched);
                childFailed.add(localFailed);
                childMissing.add(localMissing);
            }
        }

        if (childStatuses.isEmpty()) {
            return EvaluationStatus.ELIGIBLE;
        }

        if (logicalOperator.equals("ANY") || logicalOperator.equals("OR")) {
            // Under ANY (OR):
            // - If at least one is ELIGIBLE -> ELIGIBLE (promote matching branches)
            // - If none is ELIGIBLE, but at least one is INDETERMINATE -> INDETERMINATE
            // - If all are NOT_ELIGIBLE -> NOT_ELIGIBLE (promote failed branches)
            int eligibleIdx = -1;
            boolean hasIndeterminate = false;

            for (int i = 0; i < childStatuses.size(); i++) {
                if (childStatuses.get(i) == EvaluationStatus.ELIGIBLE) {
                    if (eligibleIdx == -1) eligibleIdx = i;
                    matched.addAll(childMatched.get(i));
                } else if (childStatuses.get(i) == EvaluationStatus.INDETERMINATE) {
                    hasIndeterminate = true;
                }
            }

            if (eligibleIdx != -1) {
                return EvaluationStatus.ELIGIBLE;
            } else if (hasIndeterminate) {
                for (int i = 0; i < childStatuses.size(); i++) {
                    if (childStatuses.get(i) == EvaluationStatus.INDETERMINATE) {
                        missing.addAll(childMissing.get(i));
                    }
                }
                return EvaluationStatus.INDETERMINATE;
            } else {
                for (List<String> f : childFailed) {
                    failed.addAll(f);
                }
                return EvaluationStatus.NOT_ELIGIBLE;
            }
        } else if (logicalOperator.equals("NOT")) {
            boolean hasIndeterminate = childStatuses.contains(EvaluationStatus.INDETERMINATE);
            boolean hasEligible = childStatuses.contains(EvaluationStatus.ELIGIBLE);

            if (hasIndeterminate) {
                for (List<String> m : childMissing) missing.addAll(m);
                return EvaluationStatus.INDETERMINATE;
            }
            if (hasEligible) {
                failed.add("Negated condition was satisfied (NOT group failed)");
                return EvaluationStatus.NOT_ELIGIBLE;
            }
            matched.add("Negated condition passed");
            return EvaluationStatus.ELIGIBLE;
        } else {
            // ALL / AND
            boolean hasNotEligible = false;
            boolean hasIndeterminate = false;

            for (int i = 0; i < childStatuses.size(); i++) {
                EvaluationStatus status = childStatuses.get(i);
                if (status == EvaluationStatus.NOT_ELIGIBLE) {
                    hasNotEligible = true;
                    failed.addAll(childFailed.get(i));
                } else if (status == EvaluationStatus.INDETERMINATE) {
                    hasIndeterminate = true;
                    missing.addAll(childMissing.get(i));
                } else if (status == EvaluationStatus.ELIGIBLE) {
                    matched.addAll(childMatched.get(i));
                }
            }

            if (hasNotEligible) return EvaluationStatus.NOT_ELIGIBLE;
            if (hasIndeterminate) return EvaluationStatus.INDETERMINATE;
            return EvaluationStatus.ELIGIBLE;
        }
    }

    private EvaluationStatus evaluateCondition(
            EligibilityCondition condition,
            CitizenEligibilityProfile profile,
            List<String> matched,
            List<String> failed,
            List<String> missing,
            List<ConditionEvaluationDetail> details) {

        String field = condition.getField();
        RuleOperator operator = condition.getOperator();
        String ruleValue = condition.getValue();
        String dataType = condition.getDataType() != null ? condition.getDataType().toUpperCase() : "STRING";
        String explanation = getExplanationText(condition);

        if ("LEGACY_FREE_TEXT".equalsIgnoreCase(field)) {
            String legacyExplanation = "Eligibility requires manual verification: " + explanation;
            details.add(ConditionEvaluationDetail.builder()
                    .field(field)
                    .operator(operator != null ? operator.name() : "NONE")
                    .value(ruleValue)
                    .dataType(dataType)
                    .status(EvaluationStatus.INDETERMINATE)
                    .explanation(legacyExplanation)
                    .build());
            missing.add(legacyExplanation);
            return EvaluationStatus.INDETERMINATE;
        }

        Object citizenRawValue = resolveCitizenValue(field, profile);

        if (citizenRawValue == null) {
            if (condition.isRequired()) {
                String missingMsg = "Missing required attribute: " + field;
                details.add(ConditionEvaluationDetail.builder()
                        .field(field)
                        .operator(operator != null ? operator.name() : "NONE")
                        .value(ruleValue)
                        .dataType(dataType)
                        .status(EvaluationStatus.INDETERMINATE)
                        .explanation(missingMsg)
                        .build());
                missing.add(field);
                return EvaluationStatus.INDETERMINATE;
            } else {
                return EvaluationStatus.ELIGIBLE;
            }
        }

        boolean conditionPassed;
        try {
            conditionPassed = evaluateComparison(citizenRawValue, operator, ruleValue, dataType);
        } catch (Exception e) {
            String errorMsg = "Controlled evaluation error: " + e.getMessage();
            details.add(ConditionEvaluationDetail.builder()
                    .field(field)
                    .operator(operator != null ? operator.name() : "NONE")
                    .value(ruleValue)
                    .dataType(dataType)
                    .status(EvaluationStatus.NOT_ELIGIBLE)
                    .explanation(errorMsg)
                    .build());
            failed.add(errorMsg);
            return EvaluationStatus.NOT_ELIGIBLE;
        }

        EvaluationStatus condStatus = conditionPassed ? EvaluationStatus.ELIGIBLE : EvaluationStatus.NOT_ELIGIBLE;

        details.add(ConditionEvaluationDetail.builder()
                .field(field)
                .operator(operator != null ? operator.name() : "NONE")
                .value(ruleValue)
                .dataType(dataType)
                .status(condStatus)
                .explanation(explanation)
                .build());

        if (conditionPassed) {
            matched.add(explanation);
        } else {
            failed.add("Requirement failed: " + explanation);
        }

        return condStatus;
    }

    private Object resolveCitizenValue(String field, CitizenEligibilityProfile profile) {
        if (field == null || profile == null) return null;
        String normalized = field.toUpperCase().replace("_", "");

        switch (normalized) {
            case "AGE":
                if (profile.getAge() != null) return profile.getAge();
                return resolveAgeFromDob(profile);
            case "DOB", "DATEOFBIRTH":
                return resolveDob(profile);
            case "GENDER":
                return profile.getGender();
            case "ANNUALINCOME", "INCOME":
                return profile.getAnnualIncome();
            case "OCCUPATION":
                return profile.getOccupation();
            case "STATE", "STATEORUT":
                return profile.getState();
            case "SOCIALCATEGORY", "CATEGORY", "CASTE":
                return profile.getSocialCategory();
            case "DISABILITYSTATUS":
                return profile.getDisabilityStatus();
            case "ISFARMER", "FARMER":
                if (profile.getAttributes() != null && profile.getAttributes().containsKey("isFarmer")) {
                    return profile.getAttributes().get("isFarmer");
                }
                return "Farmer".equalsIgnoreCase(profile.getOccupation());
            case "LANDHOLDINGAREA", "LANDHOLDING":
                if (profile.getAttributes() != null) {
                    return profile.getAttributes().get("landholdingArea");
                }
                return null;
            case "ISSTUDENT", "STUDENT":
                if (profile.getAttributes() != null && profile.getAttributes().containsKey("isStudent")) {
                    return profile.getAttributes().get("isStudent");
                }
                return "Student".equalsIgnoreCase(profile.getOccupation());
            case "BPLSTATUS", "BPL":
                if (profile.getAttributes() != null) {
                    return profile.getAttributes().get("bplStatus");
                }
                return null;
            case "MINORITYSTATUS", "MINORITY":
                if (profile.getAttributes() != null) {
                    return profile.getAttributes().get("minorityStatus");
                }
                return null;
            default:
                if (profile.getAttributes() != null) {
                    for (Map.Entry<String, Object> entry : profile.getAttributes().entrySet()) {
                        if (entry.getKey().replace("_", "").equalsIgnoreCase(normalized)) {
                            return entry.getValue();
                        }
                    }
                }
                return null;
        }
    }

    private Integer resolveAgeFromDob(CitizenEligibilityProfile profile) {
        Object dobObj = resolveDob(profile);
        if (dobObj instanceof LocalDate ld) {
            return Period.between(ld, LocalDate.now()).getYears();
        } else if (dobObj instanceof String s) {
            try {
                LocalDate ld = LocalDate.parse(s);
                return Period.between(ld, LocalDate.now()).getYears();
            } catch (Exception e) {}
        }
        return null;
    }

    private Object resolveDob(CitizenEligibilityProfile profile) {
        if (profile.getAttributes() != null && profile.getAttributes().containsKey("dob")) {
            return profile.getAttributes().get("dob");
        }
        return null;
    }

    private boolean evaluateComparison(Object citizenValue, RuleOperator operator, String ruleValue, String dataType) {
        if (operator == null) return false;

        switch (dataType.toUpperCase()) {
            case "NUMBER", "INTEGER", "DOUBLE", "DECIMAL":
                BigDecimal cNum = toBigDecimal(citizenValue);
                if (operator == RuleOperator.BETWEEN) {
                    String[] parts = ruleValue.split(",");
                    if (parts.length == 2) {
                        BigDecimal min = new BigDecimal(parts[0].trim());
                        BigDecimal max = new BigDecimal(parts[1].trim());
                        return cNum.compareTo(min) >= 0 && cNum.compareTo(max) <= 0;
                    }
                }
                BigDecimal rNum = new BigDecimal(ruleValue.trim());
                int cmp = cNum.compareTo(rNum);
                return switch (operator) {
                    case EQUALS, EQ -> cmp == 0;
                    case NOT_EQUALS, NEQ -> cmp != 0;
                    case GREATER_THAN, GT -> cmp > 0;
                    case GREATER_THAN_OR_EQUAL, GTE -> cmp >= 0;
                    case LESS_THAN, LT -> cmp < 0;
                    case LESS_THAN_OR_EQUAL, LTE -> cmp <= 0;
                    default -> false;
                };

            case "BOOLEAN":
                boolean cBool = toBoolean(citizenValue);
                if (operator == RuleOperator.BOOLEAN_TRUE) return cBool;
                if (operator == RuleOperator.BOOLEAN_FALSE) return !cBool;
                boolean rBool = Boolean.parseBoolean(ruleValue.trim());
                return switch (operator) {
                    case EQUALS, EQ -> cBool == rBool;
                    case NOT_EQUALS, NEQ -> cBool != rBool;
                    default -> false;
                };

            case "STRING":
            default:
                String cStr = String.valueOf(citizenValue).trim();
                return switch (operator) {
                    case EQUALS, EQ -> cStr.equalsIgnoreCase(ruleValue.trim());
                    case NOT_EQUALS, NEQ -> !cStr.equalsIgnoreCase(ruleValue.trim());
                    case IN, EQUALS_ANY -> {
                        String[] allowed = ruleValue.split(",");
                        yield Arrays.stream(allowed).anyMatch(a -> a.trim().equalsIgnoreCase(cStr));
                    }
                    case NOT_IN -> {
                        String[] disallowed = ruleValue.split(",");
                        yield Arrays.stream(disallowed).noneMatch(d -> d.trim().equalsIgnoreCase(cStr));
                    }
                    case CONTAINS -> cStr.toLowerCase().contains(ruleValue.toLowerCase().trim());
                    default -> false;
                };
        }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(val).trim());
    }

    private boolean toBoolean(Object val) {
        if (val instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(val).trim());
    }

    private String getExplanationText(EligibilityCondition cond) {
        if (cond.getDescription() != null && cond.getDescription().getEnglish() != null) {
            return cond.getDescription().getEnglish();
        }
        return cond.getField() + " " + (cond.getOperator() != null ? cond.getOperator().name() : "") + " " + cond.getValue();
    }
}
