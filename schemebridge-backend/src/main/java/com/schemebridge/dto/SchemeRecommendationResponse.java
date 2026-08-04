package com.schemebridge.dto;

import com.schemebridge.enums.EligibilityResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeRecommendationResponse {

    private SchemeCardResponse scheme;
    private int matchPercentage;
    private double priorityScore;
    private EligibilityResult eligibilityResult;
    private List<String> matchedRules;
    private List<String> failedRules;
    private List<String> missingDocuments;
    private String recommendationReason;
}
