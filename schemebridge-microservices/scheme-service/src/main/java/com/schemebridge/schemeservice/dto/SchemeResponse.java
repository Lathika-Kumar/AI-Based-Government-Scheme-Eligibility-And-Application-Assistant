package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeResponse {
    private String id;
    private String schemeCode;
    private String titleEnglish;
    private String titleTamil;
    private String descriptionEnglish;
    private String descriptionTamil;
    private String categoryCode;
    private String categoryName;
    private String departmentName;
    private String ministry;
    private String schemeType;
    private Integer launchYear;
    private String schemeUrl;
    private String applicationUrl;
    private String helplineNumber;
    private Boolean stateSpecific;
    private String applicableStates;
    private Integer version;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer priority;
    private BigDecimal popularityScore;
    private Boolean featured;
    private Boolean newlyAdded;
    private String status;
    private List<EligibilityRuleResponse> eligibilityRules;
    private List<BenefitResponse> benefits;
    private List<DocumentRequirementResponse> documents;
    private List<String> tags;
    private List<FaqResponse> faqs;
}
