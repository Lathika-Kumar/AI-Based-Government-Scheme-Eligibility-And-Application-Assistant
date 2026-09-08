package com.schemebridge.scheme.ml.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 26 User-Scheme comparison features.
 * Invariant: Every comparison field evaluates to MATCH, MISMATCH, or UNKNOWN.
 * Missing user or scheme attributes evaluate to UNKNOWN, never to MATCH.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonFeatures {

    @Builder.Default
    private FeatureMatchStatus ageMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus incomeMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus stateMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus occupationMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus categoryMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus genderMatch = FeatureMatchStatus.UNKNOWN;

    @Builder.Default
    private FeatureMatchStatus disabilityMatch = FeatureMatchStatus.UNKNOWN;
}
