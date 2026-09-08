package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationExplanation {
    private String primaryReason;
    private List<String> matchedProfileFactors;
    private String benefitRelevance;
    private String categoryMatch;
    private String geographicMatch;
    private String beneficiaryMatch;
    private String semanticRelevance;
}
