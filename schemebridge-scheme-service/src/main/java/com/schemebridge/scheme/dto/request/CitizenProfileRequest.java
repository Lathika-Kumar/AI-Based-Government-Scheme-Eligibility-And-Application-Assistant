package com.schemebridge.scheme.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request body for PUT /api/profile.
 * All fields are optional at DTO level — partial updates are supported.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfileRequest {

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    private LocalDate dob;

    @Min(value = 1,   message = "Age must be at least 1")
    @Max(value = 120, message = "Age must not exceed 120")
    private Integer age;

    @Size(max = 50, message = "Gender must not exceed 50 characters")
    private String gender;

    @Size(max = 50, message = "Marital status must not exceed 50 characters")
    private String maritalStatus;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    private String pincode;

    @Size(max = 50, message = "Residential area type must not exceed 50 characters")
    private String residentialAreaType;

    @DecimalMin(value = "0", message = "Annual income must be non-negative")
    private Double annualIncome;

    private Boolean bplStatus;

    @Size(max = 50, message = "Ration card type must not exceed 50 characters")
    private String rationCardType;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @Size(max = 50, message = "Employment status must not exceed 50 characters")
    private String employmentStatus;

    private Boolean isFarmer;

    @DecimalMin(value = "0", message = "Landholding area must be non-negative")
    private Double landholdingArea;

    private Boolean isStudent;

    @Size(max = 50, message = "Social category must not exceed 50 characters")
    private String socialCategory;

    private Boolean minorityStatus;

    @Size(max = 150, message = "Education must not exceed 150 characters")
    private String education;

    private Boolean disabilityStatus;

    @Size(max = 100, message = "Disability type must not exceed 100 characters")
    private String disabilityType;

    @Min(value = 1, message = "Disability percentage must be at least 1")
    @Max(value = 100, message = "Disability percentage must not exceed 100")
    private Integer disabilityPercentage;

    @Size(max = 50, message = "UDID number must not exceed 50 characters")
    private String udidNumber;

    /** Set to true when citizen completes all onboarding steps. */
    private Boolean onboardingComplete;

    private Integer onboardingStep;

    private Map<String, String> accessibilityPreferences;
}
