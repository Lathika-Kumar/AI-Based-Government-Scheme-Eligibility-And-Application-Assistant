package com.schemebridge.util.eligibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEvaluationResult {

    private String ruleName;
    private boolean passed;
    private int maxWeight;
    private int awardedScore;
    private String explanation;
    private String missingField;
    private String missingDocument;
}
