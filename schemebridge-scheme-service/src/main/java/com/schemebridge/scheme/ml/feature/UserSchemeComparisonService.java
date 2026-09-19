package com.schemebridge.scheme.ml.feature;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Deterministic User-Scheme criteria comparison service.
 * Invariant: Every comparison field evaluates to MATCH, MISMATCH, or UNKNOWN.
 * Missing user or scheme attributes evaluate to UNKNOWN, never to MATCH.
 */
@Service
public class UserSchemeComparisonService {

    public ComparisonFeatures compare(UserFeatures user, SchemeFeatures scheme) {
        if (user == null || scheme == null) {
            return buildAllUnknown();
        }

        FeatureMatchStatus ageMatch = compareAge(user.getAgeBucket(), scheme.getMinAge(), scheme.getMaxAge());
        FeatureMatchStatus incomeMatch = compareIncome(user.getIncomeTier(), scheme.getMaxIncome());
        FeatureMatchStatus stateMatch = compareState(user.getStateCode(), scheme.getSchemeLevel(), scheme.getStateOrUt());
        FeatureMatchStatus occMatch = compareOccupation(user.getOccupationCode(), user.getIsFarmer(), user.getIsStudent(), scheme.getEligibleOccupations());
        FeatureMatchStatus catMatch = compareCategory(user.getCategoryCode(), scheme.getEligibleCategories());
        FeatureMatchStatus genderMatch = compareGender(user.getGenderCode(), scheme.getEligibleGenders());
        FeatureMatchStatus disMatch = compareDisability(user.getDisabilityStatus(), scheme.getDisabilityApplicable());

        return ComparisonFeatures.builder()
                .ageMatch(ageMatch)
                .incomeMatch(incomeMatch)
                .stateMatch(stateMatch)
                .occupationMatch(occMatch)
                .categoryMatch(catMatch)
                .genderMatch(genderMatch)
                .disabilityMatch(disMatch)
                .build();
    }

    public FeatureMatchStatus compareAge(String ageBucket, Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) {
            return FeatureMatchStatus.UNKNOWN; // No age criteria specified
        }
        if (ageBucket == null || "UNKNOWN".equalsIgnoreCase(ageBucket)) {
            return FeatureMatchStatus.UNKNOWN;
        }

        // Determine lower and upper bounds of user's bucket
        int userMin;
        int userMax;
        switch (ageBucket) {
            case "AGE_BELOW_18": userMin = 0; userMax = 17; break;
            case "AGE_18_25":    userMin = 18; userMax = 25; break;
            case "AGE_26_35":    userMin = 26; userMax = 35; break;
            case "AGE_36_50":    userMin = 36; userMax = 50; break;
            case "AGE_51_60":    userMin = 51; userMax = 60; break;
            case "AGE_60_PLUS":  userMin = 61; userMax = 120; break;
            default: return FeatureMatchStatus.UNKNOWN;
        }

        int reqMin = minAge != null ? minAge : 0;
        int reqMax = maxAge != null ? maxAge : 150;

        if (userMin >= reqMin && userMax <= reqMax) {
            return FeatureMatchStatus.MATCH;
        } else if (userMax < reqMin || userMin > reqMax) {
            return FeatureMatchStatus.MISMATCH;
        } else {
            // Partial overlap
            return FeatureMatchStatus.MATCH;
        }
    }

    public FeatureMatchStatus compareIncome(String incomeTier, Double maxIncome) {
        if (maxIncome == null) {
            return FeatureMatchStatus.UNKNOWN; // Open income
        }
        if (incomeTier == null || "UNKNOWN".equalsIgnoreCase(incomeTier)) {
            return FeatureMatchStatus.UNKNOWN;
        }

        if ("BPL".equalsIgnoreCase(incomeTier)) {
            return FeatureMatchStatus.MATCH; // BPL is below standard scheme caps
        }

        double userApproxIncome;
        switch (incomeTier) {
            case "TIER_0_1L": userApproxIncome = 100000.0; break;
            case "TIER_1_3L": userApproxIncome = 300000.0; break;
            case "TIER_3_5L": userApproxIncome = 500000.0; break;
            case "TIER_5_8L": userApproxIncome = 800000.0; break;
            case "TIER_ABOVE_8L": userApproxIncome = 1200000.0; break;
            default: return FeatureMatchStatus.UNKNOWN;
        }

        return userApproxIncome <= maxIncome ? FeatureMatchStatus.MATCH : FeatureMatchStatus.MISMATCH;
    }

    public FeatureMatchStatus compareState(String userState, String schemeLevel, String schemeState) {
        if ("CENTRAL".equalsIgnoreCase(schemeLevel) || schemeState == null || "ALL".equalsIgnoreCase(schemeState) || schemeState.isBlank()) {
            return FeatureMatchStatus.MATCH; // Central national scheme applies across all states
        }
        if (userState == null || "UNKNOWN".equalsIgnoreCase(userState)) {
            return FeatureMatchStatus.UNKNOWN;
        }

        return userState.trim().equalsIgnoreCase(schemeState.trim())
                ? FeatureMatchStatus.MATCH
                : FeatureMatchStatus.MISMATCH;
    }

    public FeatureMatchStatus compareOccupation(String userOcc, Boolean isFarmer, Boolean isStudent, List<String> eligibleOccupations) {
        if (eligibleOccupations == null || eligibleOccupations.isEmpty()) {
            return FeatureMatchStatus.UNKNOWN;
        }
        if ((userOcc == null || "UNKNOWN".equalsIgnoreCase(userOcc)) && isFarmer == null && isStudent == null) {
            return FeatureMatchStatus.UNKNOWN;
        }

        for (String req : eligibleOccupations) {
            String r = req.toUpperCase();
            if (userOcc != null && (userOcc.equalsIgnoreCase(r) || userOcc.contains(r) || r.contains(userOcc))) {
                return FeatureMatchStatus.MATCH;
            }
            if (Boolean.TRUE.equals(isFarmer) && (r.contains("FARMER") || r.contains("AGRICULTURE") || r.contains("KISAN"))) {
                return FeatureMatchStatus.MATCH;
            }
            if (Boolean.TRUE.equals(isStudent) && (r.contains("STUDENT") || r.contains("SCHOLARSHIP"))) {
                return FeatureMatchStatus.MATCH;
            }
        }
        return FeatureMatchStatus.MISMATCH;
    }

    public FeatureMatchStatus compareCategory(String userCategory, List<String> eligibleCategories) {
        if (eligibleCategories == null || eligibleCategories.isEmpty()) {
            return FeatureMatchStatus.UNKNOWN;
        }
        if (userCategory == null || "UNKNOWN".equalsIgnoreCase(userCategory)) {
            return FeatureMatchStatus.UNKNOWN;
        }

        for (String cat : eligibleCategories) {
            String c = cat.trim().toUpperCase();
            if ("ALL".equals(c) || "ANY".equals(c)) {
                return FeatureMatchStatus.MATCH;
            }
            if (userCategory.equalsIgnoreCase(c)) {
                return FeatureMatchStatus.MATCH;
            }
        }
        return FeatureMatchStatus.MISMATCH;
    }

    public FeatureMatchStatus compareGender(String userGender, List<String> eligibleGenders) {
        if (eligibleGenders == null || eligibleGenders.isEmpty()) {
            return FeatureMatchStatus.UNKNOWN;
        }
        if (userGender == null || "UNKNOWN".equalsIgnoreCase(userGender)) {
            return FeatureMatchStatus.UNKNOWN;
        }

        for (String g : eligibleGenders) {
            String gn = g.trim().toUpperCase();
            if ("ALL".equals(gn) || "BOTH".equals(gn) || "ANY".equals(gn)) {
                return FeatureMatchStatus.MATCH;
            }
            if (userGender.equalsIgnoreCase(gn)) {
                return FeatureMatchStatus.MATCH;
            }
            if (("FEMALE".equals(gn) || "WOMEN".equals(gn) || "GIRL".equals(gn)) &&
                ("FEMALE".equalsIgnoreCase(userGender) || "WOMEN".equalsIgnoreCase(userGender))) {
                return FeatureMatchStatus.MATCH;
            }
            if (("MALE".equals(gn) || "MEN".equals(gn) || "BOY".equals(gn)) &&
                ("MALE".equalsIgnoreCase(userGender) || "MEN".equalsIgnoreCase(userGender))) {
                return FeatureMatchStatus.MATCH;
            }
        }
        return FeatureMatchStatus.MISMATCH;
    }

    public FeatureMatchStatus compareDisability(Boolean userDisability, Boolean schemeDisability) {
        if (schemeDisability == null || !schemeDisability) {
            return FeatureMatchStatus.UNKNOWN;
        }
        if (userDisability == null) {
            return FeatureMatchStatus.UNKNOWN;
        }
        return userDisability ? FeatureMatchStatus.MATCH : FeatureMatchStatus.MISMATCH;
    }

    public ComparisonFeatures buildAllUnknown() {
        return ComparisonFeatures.builder()
                .ageMatch(FeatureMatchStatus.UNKNOWN)
                .incomeMatch(FeatureMatchStatus.UNKNOWN)
                .stateMatch(FeatureMatchStatus.UNKNOWN)
                .occupationMatch(FeatureMatchStatus.UNKNOWN)
                .categoryMatch(FeatureMatchStatus.UNKNOWN)
                .genderMatch(FeatureMatchStatus.UNKNOWN)
                .disabilityMatch(FeatureMatchStatus.UNKNOWN)
                .build();
    }
}
