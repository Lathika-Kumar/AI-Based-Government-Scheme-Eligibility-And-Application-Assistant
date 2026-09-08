package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for POST /api/eligibility/evaluate.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenEligibilityEvaluationResponse {

    private String userId;
    private String citizenState;
    private Instant evaluatedAt;

    @Builder.Default
    private EvaluationSummary summary = new EvaluationSummary();

    @Builder.Default
    private List<EligibilityEvaluationResult> eligibleSchemes = new ArrayList<>();

    @Builder.Default
    private List<EligibilityEvaluationResult> insufficientDataSchemes = new ArrayList<>();

    @Builder.Default
    private List<EligibilityEvaluationResult> notEligibleSchemes = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationSummary {
        private int totalEvaluated;
        private int eligibleCount;
        private int insufficientDataCount;
        private int notEligibleCount;
    }
}
