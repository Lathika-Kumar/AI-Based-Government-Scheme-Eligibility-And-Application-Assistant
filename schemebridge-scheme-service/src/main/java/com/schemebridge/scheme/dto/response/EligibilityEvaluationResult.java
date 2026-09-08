package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Result contract returned by Phase 6B Deterministic Eligibility Engine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityEvaluationResult {

    private String schemeId;
    private String schemeCode;
    private String slug;
    private String schemeTitle;
    private String schemeLevel;
    private String categoryCode;
    private String categoryName;
    private EligibilityStatus status;
    private Double confidenceScore;

    @Builder.Default
    private List<String> passedConditions = new ArrayList<>();

    @Builder.Default
    private List<String> failedConditions = new ArrayList<>();

    @Builder.Default
    private List<String> missingAttributes = new ArrayList<>();

    @Builder.Default
    private List<String> verifiedAttributesUsed = new ArrayList<>();

    private String explanation;
}
