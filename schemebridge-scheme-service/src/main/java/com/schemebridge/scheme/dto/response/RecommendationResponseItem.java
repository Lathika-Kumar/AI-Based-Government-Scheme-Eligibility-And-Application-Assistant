package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.MultilingualText;
import com.schemebridge.scheme.document.SchemeLevel;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse.ConditionEvaluationDetail;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponseItem {
    private String schemeId;
    private String schemeCode;
    private String slug;
    private MultilingualText title;
    private MultilingualText shortDescription;
    private SchemeLevel schemeLevel;
    private String stateOrUt;
    private String beneficiaryType;
    private String schemeType;
    private double matchScore;
    private RecommendationCategory recommendationCategory;
    private String eligibilityStatus; // "ELIGIBLE", "NOT_ELIGIBLE", "INDETERMINATE"
    private List<String> matchedConditions;
    private List<String> failedConditions;
    private List<String> missingInformation;
    private List<String> reasons;
    private List<ConditionEvaluationDetail> details;
    private Double semanticScore;
    private String rankingMethod;
    private String modelVersion;
    private java.time.Instant deadline;
}
