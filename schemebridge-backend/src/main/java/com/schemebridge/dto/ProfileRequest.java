package com.schemebridge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {

    private String fullName;

    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 110, message = "Age cannot exceed 110")
    private Integer age;

    private String gender;
    private String maritalStatus;

    private String state;
    private String district;
    private String cityOrVillage;

    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be exactly 6 digits starting with 1-9")
    private String pincode;

    @DecimalMin(value = "0.0", message = "Annual income cannot be negative")
    private BigDecimal annualIncome;

    private String category;
    private String occupation;
    private String employmentStatus;
    private Boolean disabilityStatus;

    private String qualification;

    @Pattern(regexp = "^[2-9]{1}[0-9]{11}$", message = "Aadhaar number must be a valid 12-digit number")
    private String aadhaarNumber;

    @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$", message = "PAN must follow standard 10-character format (e.g. ABCDE1234F)")
    private String panNumber;
}
