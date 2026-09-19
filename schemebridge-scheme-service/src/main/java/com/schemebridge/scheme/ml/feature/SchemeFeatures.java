package com.schemebridge.scheme.ml.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Canonical Scheme Features representing criteria extracted from authoritative scheme data.
 * Criteria are extracted from Master Scheme and SchemeVerifiedData without fabrication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeFeatures {

    private String schemeCode;
    private String schemeCategory;
    private String benefitCategory;
    private String schemeLevel; // CENTRAL, STATE
    private String stateOrUt;
    private Integer minAge;
    private Integer maxAge;
    private Double maxIncome;
    private List<String> eligibleOccupations;
    private List<String> eligibleCategories;
    private List<String> eligibleGenders;
    private Boolean disabilityApplicable;
    private Boolean isFarmer;
    private Boolean isStudent;
    private Boolean bplStatus;
    private String beneficiaryType;
}
