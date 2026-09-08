package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankedSchemeItem {
    private int rank;
    private String schemeCode;
    private String slug;
    private String schemeTitle;
    private String shortDescription;
    private String schemeLevel;
    private String stateOrUt;
    private String categoryCode;
    private String categoryName;
    private String beneficiaryType;
    private String schemeType;
    private double recommendationScore; // Normalized 0.0 to 1.0
    private String eligibilityStatus;    // Always "ELIGIBLE"
    private Double confidenceScore;      // 1.0 (deterministic)
    private List<String> passedConditions;
    private List<String> verifiedAttributesUsed;
    private RecommendationExplanation explanation;
    private List<String> reasons;
    private com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector featureVector;
    private List<String> requiredDocuments;
    private List<DocumentChecklistItemResponse> checklist;
    private List<String> missingDocuments;
    private List<String> applicationSteps;
    private String applicationUrl;
    private List<String> benefits;
    private String department;
    private String ministry;
    private String expectedProcessingTime;
    private String applicationStatus; // e.g. "NOT_APPLIED", "DRAFT", "SUBMITTED", "APPROVED"
    private String applicationId;

    public boolean isEligible() {
        return "ELIGIBLE".equalsIgnoreCase(eligibilityStatus);
    }

    public double getMatchScore() {
        return recommendationScore;
    }

    public String getSchemeName() {
        return schemeTitle;
    }
}

