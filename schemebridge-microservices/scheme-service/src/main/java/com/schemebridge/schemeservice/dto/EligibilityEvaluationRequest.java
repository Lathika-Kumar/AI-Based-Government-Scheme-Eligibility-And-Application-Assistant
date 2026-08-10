package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for the evaluate-eligibility endpoint called by Citizen Service.
 * Citizen Service passes citizen profile criteria; Scheme Service evaluates against dynamic Oracle rules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityEvaluationRequest {
    private String authUserId;
    private String category;     // OBC, SC, ST, GENERAL, EWS
    private String gender;
    private Integer age;
    private BigDecimal annualIncome;
    private String state;
    private String district;
    private String occupationType;
    private Boolean isFarmer;
    private Boolean isDisabled;
    private Boolean isMinority;
    private Boolean isStudent;
    private Boolean isWidow;
    private Boolean isSeniorCitizen;
    private String religion;
    private String community;
    private String maritalStatus; // SINGLE, MARRIED, WIDOWED, DIVORCED
    private List<String> preferredCategories;
}
