package com.schemebridge.scheme.ml.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Versioned, serializable, and privacy-safe User-Scheme Feature Vector.
 * Designed for offline Learning-to-Rank training data preparation when sufficient legitimate outcomes exist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSchemeFeatureVector {

    @Builder.Default
    private String schemaVersion = "1.0.0";

    private UserFeatures userFeatures;
    private SchemeFeatures schemeFeatures;
    private ComparisonFeatures comparisonFeatures;
    private Map<String, Object> rankingContext;
}
