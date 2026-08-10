package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityEvaluationResponse {
    private String authUserId;
    private Double eligibilityScore;       // 0-100
    private Integer matchedSchemesCount;
    private List<SchemeResponse> matchedSchemes;
    private List<String> evaluationNotes;  // e.g. "Matched on CATEGORY=OBC, INCOME<=180000"
    private Long evaluatedAt;
}
