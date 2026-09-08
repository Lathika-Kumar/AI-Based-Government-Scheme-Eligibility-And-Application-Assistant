package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.EvaluationStatus;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityEvaluationResponse {
    private String schemeId;
    private String schemeCode;
    private EvaluationStatus status;
    
    @Builder.Default
    private List<String> matchedConditions = new ArrayList<>();
    
    @Builder.Default
    private List<String> failedConditions = new ArrayList<>();
    
    @Builder.Default
    private List<String> missingInformation = new ArrayList<>();
    
    @Builder.Default
    private List<ConditionEvaluationDetail> details = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConditionEvaluationDetail {
        private String field;
        private String operator;
        private String value;
        private String dataType;
        private EvaluationStatus status;
        private String explanation;
    }
}
