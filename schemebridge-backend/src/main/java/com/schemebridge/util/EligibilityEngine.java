package com.schemebridge.util;

import com.schemebridge.dto.EligibilityCheckResponse;
import com.schemebridge.dto.SchemeCardResponse;
import com.schemebridge.dto.SchemeRecommendationResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import com.schemebridge.enums.EligibilityResult;
import com.schemebridge.util.eligibility.EligibilityRuleEvaluator;
import com.schemebridge.util.eligibility.RuleEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EligibilityEngine {

    private final List<EligibilityRuleEvaluator> evaluators;

    public EligibilityCheckResponse evaluateEligibility(User user, CitizenProfile profile, Scheme scheme) {
        if (scheme == null) {
            return EligibilityCheckResponse.builder()
                    .eligible(false)
                    .matchPercentage(0)
                    .eligibilityResult(EligibilityResult.NOT_ELIGIBLE)
                    .explanation("Scheme does not exist")
                    .build();
        }

        int totalScore = 0;
        List<String> matchedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();
        List<String> missingFields = new ArrayList<>();
        List<String> missingDocuments = new ArrayList<>();
        Map<String, Boolean> criteriaBreakdown = new HashMap<>();

        for (EligibilityRuleEvaluator evaluator : evaluators) {
            RuleEvaluationResult result = evaluator.evaluate(user, profile, scheme);
            totalScore += result.getAwardedScore();
            criteriaBreakdown.put(result.getRuleName(), result.isPassed());

            if (result.isPassed()) {
                matchedRules.add(result.getExplanation());
            } else {
                failedRules.add(result.getExplanation());
                if (result.getMissingField() != null) {
                    missingFields.add(result.getMissingField());
                }
                if (result.getMissingDocument() != null) {
                    missingDocuments.add(result.getMissingDocument());
                }
            }
        }

        int matchPercentage = Math.min(100, Math.max(0, totalScore));

        EligibilityResult resultEnum;
        if (matchPercentage >= 80) {
            resultEnum = EligibilityResult.ELIGIBLE;
        } else if (matchPercentage >= 50) {
            resultEnum = EligibilityResult.PARTIALLY_ELIGIBLE;
        } else {
            resultEnum = EligibilityResult.NOT_ELIGIBLE;
        }

        boolean isEligible = (resultEnum == EligibilityResult.ELIGIBLE || resultEnum == EligibilityResult.PARTIALLY_ELIGIBLE);

        String explanation = generateExplanationSummary(resultEnum, matchPercentage, matchedRules, failedRules);

        return EligibilityCheckResponse.builder()
                .schemeId(scheme.getId())
                .schemeName(scheme.getSchemeName())
                .eligible(isEligible)
                .matchPercentage(matchPercentage)
                .eligibilityResult(resultEnum)
                .criteriaBreakdown(criteriaBreakdown)
                .matchedRules(matchedRules)
                .failedRules(failedRules)
                .missingFields(missingFields)
                .missingDocuments(missingDocuments)
                .explanation(explanation)
                .build();
    }

    public SchemeRecommendationResponse evaluateRecommendation(User user, CitizenProfile profile, Scheme scheme, SchemeCardResponse cardResponse) {
        EligibilityCheckResponse check = evaluateEligibility(user, profile, scheme);

        // Priority Score Formula: (matchPercentage * 0.70) + (featured ? 20 : 0) + (priority * 10)
        double featuredBonus = Boolean.TRUE.equals(scheme.getFeatured()) ? 20.0 : 0.0;
        int priorityVal = scheme.getPriority() != null ? scheme.getPriority() : 1;
        double priorityScore = (check.getMatchPercentage() * 0.70) + featuredBonus + (priorityVal * 10.0);

        if (cardResponse != null) {
            cardResponse.setMatchPercentage(check.getMatchPercentage());
        }

        String topReason = generateTopRecommendationReason(user, profile, scheme, check.getMatchedRules());

        return SchemeRecommendationResponse.builder()
                .scheme(cardResponse)
                .matchPercentage(check.getMatchPercentage())
                .priorityScore(priorityScore)
                .eligibilityResult(check.getEligibilityResult())
                .matchedRules(check.getMatchedRules())
                .failedRules(check.getFailedRules())
                .missingDocuments(check.getMissingDocuments())
                .recommendationReason(topReason)
                .build();
    }

    private String generateExplanationSummary(EligibilityResult result, int score, List<String> matched, List<String> failed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Profile match score: ").append(score).append("% (").append(result.name()).append("). ");
        if (!matched.isEmpty()) {
            sb.append("Matched criteria: ").append(String.join(", ", matched)).append(". ");
        }
        if (!failed.isEmpty()) {
            sb.append("Unmatched/Pending criteria: ").append(String.join(", ", failed)).append(".");
        }
        return sb.toString();
    }

    private String generateTopRecommendationReason(User user, CitizenProfile profile, Scheme scheme, List<String> matchedRules) {
        if (matchedRules == null || matchedRules.isEmpty()) {
            return "General welfare scheme suitable for citizens.";
        }
        return "Recommended because: " + String.join(" • ", matchedRules.subList(0, Math.min(3, matchedRules.size())));
    }
}
