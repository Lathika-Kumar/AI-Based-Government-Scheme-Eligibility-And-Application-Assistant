package com.schemebridge.scheme.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedRecommendationResponse {
    private int totalSchemesEvaluated;
    private int eligibleCount;
    private int nearMatchCount;
    private int indeterminateCount;
    private List<RecommendationResponseItem> recommendations;
}
