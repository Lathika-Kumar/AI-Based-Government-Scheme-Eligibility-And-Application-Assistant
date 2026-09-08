package com.schemebridge.scheme.ml.feature;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds canonical, versioned, and reproducible UserSchemeFeatureVector objects.
 * Two identical inputs produce identical feature vectors.
 * Enforces deterministic ordering via LinkedHashMap and sorted list representations.
 */
@Component
public class FeatureVectorBuilder {

    public static final String CURRENT_SCHEMA_VERSION = "1.0.0";

    public UserSchemeFeatureVector buildVector(
            UserFeatures userFeatures,
            SchemeFeatures schemeFeatures,
            ComparisonFeatures comparisonFeatures,
            Integer currentRank,
            Double currentScore,
            String modelVersion
    ) {
        // Deterministic rankingContext using LinkedHashMap with strict key insertion order
        Map<String, Object> rankingContext = new LinkedHashMap<>();
        rankingContext.put("currentRank", currentRank != null ? currentRank : 0);
        rankingContext.put("currentScore", currentScore != null ? currentScore : 0.0);
        rankingContext.put("modelVersion", modelVersion != null ? modelVersion : "UNKNOWN");

        // Ensure schemeFeatures list collections are deterministically ordered
        SchemeFeatures normalizedSchemeFeatures = schemeFeatures;
        if (schemeFeatures != null) {
            normalizedSchemeFeatures = SchemeFeatures.builder()
                    .schemeCode(schemeFeatures.getSchemeCode())
                    .schemeCategory(schemeFeatures.getSchemeCategory())
                    .benefitCategory(schemeFeatures.getBenefitCategory())
                    .schemeLevel(schemeFeatures.getSchemeLevel())
                    .stateOrUt(schemeFeatures.getStateOrUt())
                    .minAge(schemeFeatures.getMinAge())
                    .maxAge(schemeFeatures.getMaxAge())
                    .maxIncome(schemeFeatures.getMaxIncome())
                    .eligibleOccupations(sortListDeterministically(schemeFeatures.getEligibleOccupations()))
                    .eligibleCategories(sortListDeterministically(schemeFeatures.getEligibleCategories()))
                    .eligibleGenders(sortListDeterministically(schemeFeatures.getEligibleGenders()))
                    .disabilityApplicable(schemeFeatures.getDisabilityApplicable())
                    .build();
        }

        return UserSchemeFeatureVector.builder()
                .schemaVersion(CURRENT_SCHEMA_VERSION)
                .userFeatures(userFeatures)
                .schemeFeatures(normalizedSchemeFeatures)
                .comparisonFeatures(comparisonFeatures)
                .rankingContext(rankingContext)
                .build();
    }

    private List<String> sortListDeterministically(List<String> input) {
        if (input == null) return null;
        List<String> copy = new ArrayList<>(input);
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }
}
