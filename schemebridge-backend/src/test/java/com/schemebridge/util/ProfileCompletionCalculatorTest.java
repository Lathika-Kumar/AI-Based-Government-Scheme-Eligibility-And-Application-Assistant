package com.schemebridge.util;

import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.User;
import com.schemebridge.enums.ProfileCompletionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCompletionCalculatorTest {

    private ProfileCompletionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ProfileCompletionCalculator();
    }

    @Test
    @DisplayName("Empty Profile returns INCOMPLETE status and low score")
    void testEmptyProfile() {
        User user = User.builder().fullName("").build();
        CitizenProfile profile = CitizenProfile.builder().build();

        ProfileCompletionResponse result = calculator.calculateCompletion(user, profile);

        assertEquals(ProfileCompletionStatus.INCOMPLETE, result.getStatus());
        assertEquals(0, result.getCompletionPercentage());
        assertFalse(result.isOnboardingCompleted());
    }

    @Test
    @DisplayName("Partial Profile returns PARTIALLY_COMPLETED status when score is >= 50%")
    void testPartialProfile() {
        User user = User.builder().fullName("Rajesh Patel").build();
        CitizenProfile profile = CitizenProfile.builder()
                .gender("Male")
                .state("Gujarat")
                .district("Ahmedabad")
                .annualIncome(new BigDecimal("180000"))
                .qualification("Graduate")
                .maritalStatus("Married")
                .build();

        ProfileCompletionResponse result = calculator.calculateCompletion(user, profile);

        assertEquals(ProfileCompletionStatus.PARTIALLY_COMPLETED, result.getStatus());
        assertTrue(result.getCompletionPercentage() >= 50);
        assertFalse(result.isOnboardingCompleted()); // missing category & occupation
    }

    @Test
    @DisplayName("Complete Profile returns COMPLETED status and 100%")
    void testCompleteProfile() {
        User user = User.builder().fullName("Rajesh Patel").onboardingCompleted(true).build();
        CitizenProfile profile = CitizenProfile.builder()
                .dateOfBirth(LocalDate.of(1992, 5, 15))
                .gender("Male")
                .maritalStatus("Married")
                .state("Gujarat")
                .district("Ahmedabad")
                .cityOrVillage("Sanand")
                .pincode("382110")
                .annualIncome(new BigDecimal("180000"))
                .category("OBC")
                .occupation("Farmer")
                .employmentStatus("Self-Employed")
                .qualification("Higher Secondary")
                .aadhaarNumber("999988887777")
                .panNumber("ABCDE1234F")
                .build();

        ProfileCompletionResponse result = calculator.calculateCompletion(user, profile);

        assertEquals(ProfileCompletionStatus.COMPLETED, result.getStatus());
        assertEquals(100, result.getCompletionPercentage());
        assertTrue(result.isOnboardingCompleted());
    }
}
