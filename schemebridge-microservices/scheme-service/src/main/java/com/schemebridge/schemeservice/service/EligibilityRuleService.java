package com.schemebridge.schemeservice.service;

import com.schemebridge.schemeservice.dto.*;
import com.schemebridge.schemeservice.entity.Scheme;
import com.schemebridge.schemeservice.entity.SchemeEligibilityRule;
import com.schemebridge.schemeservice.repository.SchemeEligibilityRuleRepository;
import com.schemebridge.schemeservice.repository.SchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EligibilityRuleService evaluates citizen profile data against dynamically stored
 * Oracle eligibility rules for each scheme. No business rules are hardcoded.
 *
 * Called by: POST /api/v1/schemes/evaluate-eligibility (from Citizen Service via EligibilityServiceClient)
 */
@Service
@RequiredArgsConstructor
public class EligibilityRuleService {

    private final SchemeRepository schemeRepository;
    private final SchemeEligibilityRuleRepository ruleRepository;
    private final SchemeService schemeService;

    @Transactional(readOnly = true)
    public EligibilityEvaluationResponse evaluateEligibility(EligibilityEvaluationRequest request) {
        List<Scheme> allActive = schemeRepository.findByStatusOrderByPriorityAscPopularityScoreDesc("ACTIVE");
        List<SchemeResponse> matched = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        for (Scheme scheme : allActive) {
            List<SchemeEligibilityRule> rules = ruleRepository
                    .findBySchemeIdAndStatusOrderByDisplayOrderAsc(scheme.getId(), "ACTIVE");

            if (rules.isEmpty()) {
                // No rules = universally applicable
                matched.add(schemeService.mapToResponse(scheme));
                notes.add("[" + scheme.getSchemeCode() + "] No eligibility rules – universally applicable");
                continue;
            }

            boolean eligible = rules.stream().allMatch(rule -> evaluateRule(rule, request));
            if (eligible) {
                matched.add(schemeService.mapToResponse(scheme));
                String ruleDesc = rules.stream().map(r -> r.getRuleType() + " " + r.getOperator() + " " + r.getValueString()).collect(Collectors.joining(", "));
                notes.add("[" + scheme.getSchemeCode() + "] Matched: " + ruleDesc);
            }
        }

        int count = matched.size();
        double score = count == 0 ? 0.0 : Math.min(100.0, count * 5.0 + 50.0);

        return EligibilityEvaluationResponse.builder()
                .authUserId(request.getAuthUserId())
                .eligibilityScore(score)
                .matchedSchemesCount(count)
                .matchedSchemes(matched)
                .evaluationNotes(notes)
                .evaluatedAt(System.currentTimeMillis())
                .build();
    }

    private boolean evaluateRule(SchemeEligibilityRule rule, EligibilityEvaluationRequest req) {
        try {
            return switch (rule.getRuleType()) {
                case "CATEGORY" -> evaluateStringIn(rule, req.getCategory());
                case "GENDER" -> evaluateStringIn(rule, req.getGender());
                case "STATE" -> evaluateStringIn(rule, req.getState());
                case "RELIGION" -> evaluateStringIn(rule, req.getReligion());
                case "COMMUNITY" -> evaluateStringIn(rule, req.getCommunity());
                case "OCCUPATION" -> evaluateStringIn(rule, req.getOccupationType());
                case "MARITAL_STATUS" -> evaluateStringIn(rule, req.getMaritalStatus());
                case "INCOME" -> evaluateNumericRange(rule, req.getAnnualIncome());
                case "AGE" -> evaluateNumericRange(rule, req.getAge() != null ? BigDecimal.valueOf(req.getAge()) : null);
                case "FARMER" -> evaluateBoolean(rule, req.getIsFarmer());
                case "DISABLED" -> evaluateBoolean(rule, req.getIsDisabled());
                case "MINORITY" -> evaluateBoolean(rule, req.getIsMinority());
                case "STUDENT" -> evaluateBoolean(rule, req.getIsStudent());
                case "WIDOW" -> evaluateBoolean(rule, req.getIsWidow());
                case "SENIOR_CITIZEN" -> evaluateBoolean(rule, req.getIsSeniorCitizen());
                default -> true; // Unknown rule type – skip (don't exclude citizen)
            };
        } catch (Exception e) {
            return true; // Safe default – don't penalize citizen for rule evaluation error
        }
    }

    private boolean evaluateStringIn(SchemeEligibilityRule rule, String value) {
        if (value == null || rule.getValueString() == null) return true;
        List<String> allowedValues = Arrays.asList(rule.getValueString().split(","));
        return allowedValues.stream().anyMatch(v -> v.trim().equalsIgnoreCase(value.trim()));
    }

    private boolean evaluateNumericRange(SchemeEligibilityRule rule, BigDecimal value) {
        if (value == null) return true;
        if ("LTE".equals(rule.getOperator()) && rule.getValueNumberMax() != null) {
            return value.compareTo(rule.getValueNumberMax()) <= 0;
        }
        if ("GTE".equals(rule.getOperator()) && rule.getValueNumberMin() != null) {
            return value.compareTo(rule.getValueNumberMin()) >= 0;
        }
        if ("BETWEEN".equals(rule.getOperator()) && rule.getValueNumberMin() != null && rule.getValueNumberMax() != null) {
            return value.compareTo(rule.getValueNumberMin()) >= 0 && value.compareTo(rule.getValueNumberMax()) <= 0;
        }
        if ("EQ".equals(rule.getOperator()) && rule.getValueNumberMin() != null) {
            return value.compareTo(rule.getValueNumberMin()) == 0;
        }
        return true;
    }

    private boolean evaluateBoolean(SchemeEligibilityRule rule, Boolean value) {
        if (value == null || rule.getValueString() == null) return true;
        boolean expected = Boolean.parseBoolean(rule.getValueString().trim());
        return value == expected;
    }
}
