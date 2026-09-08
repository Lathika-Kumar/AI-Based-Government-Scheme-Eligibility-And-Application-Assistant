package com.schemebridge.scheme.ml.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Privacy-safe User Features representing a citizen profile.
 * Zero-PII Invariant: MUST NOT contain fullName, email, mobileNumber,
 * aadhaarNumber, address, pincode, udidNumber, or any direct identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFeatures {

    @Builder.Default
    private String ageBucket = "UNKNOWN";

    private String stateCode;
    private String district;
    private String occupationCode;

    @Builder.Default
    private String incomeTier = "UNKNOWN";

    private String categoryCode;
    private String genderCode;
    private Boolean disabilityStatus;
    private Boolean isFarmer;
    private Boolean isStudent;
    private Boolean bplStatus;
    private String educationLevel;
}
