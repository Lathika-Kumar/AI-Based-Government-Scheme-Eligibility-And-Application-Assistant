package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.MultilingualText;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse.ConditionEvaluationDetail;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeEvaluationSummary {
    private String schemeId;
    private String schemeCode;
    private String slug;
    private MultilingualText title;
    private MultilingualText description;

    @Builder.Default
    private List<String> matchedConditions = new ArrayList<>();

    @Builder.Default
    private List<String> failedConditions = new ArrayList<>();

    @Builder.Default
    private List<String> missingInformation = new ArrayList<>();

    @Builder.Default
    private List<ConditionEvaluationDetail> details = new ArrayList<>();
}
