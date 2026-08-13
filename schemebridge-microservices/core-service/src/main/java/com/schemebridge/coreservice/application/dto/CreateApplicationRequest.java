package com.schemebridge.coreservice.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {

    @NotBlank(message = "Auth User ID is required")
    private String authUserId;

    private String citizenProfileId;

    @NotBlank(message = "Scheme ID is required")
    private String schemeId;

    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @NotBlank(message = "Scheme name is required")
    private String schemeName;

    private String departmentId;
    private String departmentName;

    // Eligibility snapshot data at time of application
    private String category;
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
    private Double eligibilityScore;
    private List<String> matchedRules;
    private String schemeVersion;

    private String remarks;
}
