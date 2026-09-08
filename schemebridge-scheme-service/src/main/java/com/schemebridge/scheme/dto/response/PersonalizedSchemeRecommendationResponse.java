package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedSchemeRecommendationResponse {
    private String userId;
    private String citizenState;
    private int totalCatalogEvaluated;
    private int eligibleCandidatesFound;
    private int totalRecommendationsReturned;
    private int page;
    private int size;
    private Instant generatedAt;
    private String modelFamily; // "Multi-Criteria Feature Utility & Vector Space Model"
    private String modelVersion; // "v1.0-deterministic-gated"
    private String rankingMethod; // "HYBRID_SEMANTIC" or "DETERMINISTIC_FALLBACK"
    private Boolean fallbackUsed;
    @Builder.Default
    private String fallbackModel = "1.0.0-deterministic";
    @Builder.Default
    private String eligibilityAuthority = "EligibilityEngine";
    private List<RankedSchemeItem> recommendations;

    public int getTotalEligibleSchemes() {
        return eligibleCandidatesFound;
    }
}

