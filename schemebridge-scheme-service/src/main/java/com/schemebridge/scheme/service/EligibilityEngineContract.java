package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;

/**
 * Architectural contract interface for the Phase 6B Deterministic Eligibility Engine.
 *
 * Guarantees strict separation between:
 *   1. Deterministic Boolean rule evaluation (Phase 6B)
 *   2. AI / ML ranking & recommendation (Phase 6C)
 */
public interface EligibilityEngineContract {

    /**
     * Evaluates a citizen eligibility profile against a specific scheme's criteria.
     *
     * @param profile Verified citizen demographic and eligibility attributes
     * @param scheme  Master scheme document with eligibility rules
     * @return Deterministic result (ELIGIBLE, NOT_ELIGIBLE, or INSUFFICIENT_DATA)
     */
    EligibilityEvaluationResult evaluate(CitizenEligibilityProfile profile, Scheme scheme);

    /**
     * Evaluates a citizen eligibility profile against a specific scheme's criteria
     * taking into account authoritative canonical SchemeVerifiedData.
     *
     * @param profile      Verified citizen demographic and eligibility attributes
     * @param scheme       Master scheme document with eligibility rules
     * @param verifiedData Canonical SchemeVerifiedData with authoritative criteria
     * @return Deterministic result (ELIGIBLE, NOT_ELIGIBLE, or INSUFFICIENT_DATA)
     */
    default EligibilityEvaluationResult evaluate(
            CitizenEligibilityProfile profile,
            Scheme scheme,
            com.schemebridge.scheme.document.SchemeVerifiedData verifiedData) {
        return evaluate(profile, scheme);
    }
}
