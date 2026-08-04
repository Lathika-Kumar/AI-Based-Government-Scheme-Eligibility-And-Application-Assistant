package com.schemebridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private String id;
    private String userId;
    private String email;
    private String fullName;
    private String phoneNumber;

    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String maritalStatus;

    private String state;
    private String district;
    private String cityOrVillage;
    private String pincode;

    private BigDecimal annualIncome;
    private String category;
    private String occupation;
    private String employmentStatus;
    private Boolean disabilityStatus;

    private String qualification;

    // Masked PII
    private String maskedAadhaarNumber;
    private String maskedPanNumber;

    // Status & Audit Flags
    private Integer profileVersion;
    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
    private LocalDateTime profileCompletedAt;
    private Boolean onboardingCompleted;
    private Instant createdAt;
}
