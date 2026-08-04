package com.schemebridge.util.eligibility;

import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;

public interface EligibilityRuleEvaluator {
    RuleEvaluationResult evaluate(User user, CitizenProfile profile, Scheme scheme);
    String getRuleName();
}
