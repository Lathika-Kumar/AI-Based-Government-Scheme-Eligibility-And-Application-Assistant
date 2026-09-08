package com.schemebridge.scheme.document;

import lombok.*;

/**
 * Snapshot of the recommendation engine state at the exact moment of citizen interaction.
 * Used for offline model evaluation, counterfactual analysis, and Learning-to-Rank training.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationContextSnapshot {
    private String modelVersion;
    private Integer recommendationRank;
    private Double recommendationScore;
    private String eligibilityStatus;
}
