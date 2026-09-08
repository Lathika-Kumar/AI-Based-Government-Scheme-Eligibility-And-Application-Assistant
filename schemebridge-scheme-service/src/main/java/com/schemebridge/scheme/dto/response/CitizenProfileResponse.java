package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.VerifiedAttribute;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Response body for GET /api/profile and PUT /api/profile.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfileResponse {

    private String id;
    private String userId;
    private String displayName;
    private LocalDate dob;
    private Integer age;
    private String gender;
    private String maritalStatus;
    private String state;
    private String district;
    private String pincode;
    private String residentialAreaType;
    private Double annualIncome;
    private Boolean bplStatus;
    private String rationCardType;
    private String occupation;
    private String employmentStatus;
    private Boolean isFarmer;
    private Double landholdingArea;
    private Boolean isStudent;
    private String socialCategory;
    private Boolean minorityStatus;
    private String education;
    private Boolean disabilityStatus;
    private String disabilityType;
    private Integer disabilityPercentage;
    private String udidNumber;

    /** Map of attribute verification states and provenance. */
    private Map<String, VerifiedAttribute<?>> verifiedAttributes;

    private Boolean onboardingComplete;
    private String onboardingStatus;
    private Integer onboardingStep;
    private Map<String, String> accessibilityPreferences;
    private Instant createdAt;
    private Instant updatedAt;
}
