package com.schemebridge.scheme.ml.dataset;

import com.schemebridge.scheme.ml.feature.UserSchemeFeatureVector;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Standardized Learning-to-Rank (LTR) Training Pair (x_i, y_i).
 * Invariant: Can ONLY be instantiated for schemes that have passed statutory eligibility.
 */
@Getter
@ToString
public class TrainingPair {

    private final String pairId;
    private final String sessionId;
    private final String schemeCode;
    private final UserSchemeFeatureVector featureVector;
    private final RelevanceGrade relevanceGrade;
    private final int relevanceScore;
    private final boolean isStatutoryEligible;
    private final String split;
    private final long timestamp;

    @Builder
    public TrainingPair(
            String pairId,
            String sessionId,
            String schemeCode,
            UserSchemeFeatureVector featureVector,
            RelevanceGrade relevanceGrade,
            boolean isStatutoryEligible,
            String split,
            long timestamp
    ) {
        if (!isStatutoryEligible) {
            throw new IllegalStateException("Statutory Eligibility Invariant Violated: Cannot construct TrainingPair for an ineligible scheme. " +
                    "EligibilityEngine.evaluate() must confirm ELIGIBLE status before feature extraction or training pair generation.");
        }
        this.pairId = Objects.requireNonNull(pairId, "pairId must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.schemeCode = Objects.requireNonNull(schemeCode, "schemeCode must not be null");
        this.featureVector = Objects.requireNonNull(featureVector, "featureVector must not be null");
        this.relevanceGrade = relevanceGrade != null ? relevanceGrade : RelevanceGrade.IMPRESSED_UNENGAGED;
        this.relevanceScore = this.relevanceGrade.getGrade();
        this.isStatutoryEligible = true;
        this.split = split != null ? split : "TRAIN";
        this.timestamp = timestamp;
    }
}
