package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkEligibilityEvaluationResponse {
    @Builder.Default
    private List<SchemeEvaluationSummary> eligible = new ArrayList<>();

    @Builder.Default
    private List<SchemeEvaluationSummary> ineligible = new ArrayList<>();

    @Builder.Default
    private List<SchemeEvaluationSummary> indeterminate = new ArrayList<>();
}
