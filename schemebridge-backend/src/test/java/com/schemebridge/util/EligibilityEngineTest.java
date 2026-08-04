package com.schemebridge.util;

import com.schemebridge.dto.EligibilityCheckResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.Scheme;
import com.schemebridge.entity.User;
import com.schemebridge.enums.EligibilityResult;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.util.eligibility.AgeRule;
import com.schemebridge.util.eligibility.CategoryRule;
import com.schemebridge.util.eligibility.DocumentRule;
import com.schemebridge.util.eligibility.GenderRule;
import com.schemebridge.util.eligibility.IncomeRule;
import com.schemebridge.util.eligibility.OccupationRule;
import com.schemebridge.util.eligibility.StateRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityEngineTest {

    private EligibilityEngine eligibilityEngine;

    @BeforeEach
    void setUp() {
        eligibilityEngine = new EligibilityEngine(List.of(
                new AgeRule(),
                new IncomeRule(),
                new StateRule(),
                new CategoryRule(),
                new GenderRule(),
                new OccupationRule(),
                new DocumentRule()
        ));
    }

    @Test
    @DisplayName("Fully matching profile yields ELIGIBLE status with 100% score")
    void testFullMatchEligibility() {
        User user = User.builder().fullName("Rajesh Patel").build();
        CitizenProfile profile = CitizenProfile.builder()
                .age(30)
                .gender("Male")
                .state("Gujarat")
                .annualIncome(new BigDecimal("180000"))
                .category("OBC")
                .occupation("Farmer")
                .aadhaarNumber("999988887777")
                .panNumber("ABCDE1234F")
                .build();

        Scheme scheme = Scheme.builder()
                .schemeName("PM Kisan")
                .schemeType(SchemeType.CENTRAL)
                .minimumAge(18)
                .maximumAge(60)
                .maximumIncome(new BigDecimal("300000"))
                .applicableStates(List.of("All India"))
                .allowedCategories(List.of("ALL"))
                .allowedGenders(List.of("ALL"))
                .allowedOccupations(List.of("Farmer"))
                .requiredDocuments(List.of("Aadhaar Card"))
                .build();

        EligibilityCheckResponse response = eligibilityEngine.evaluateEligibility(user, profile, scheme);

        assertEquals(EligibilityResult.ELIGIBLE, response.getEligibilityResult());
        assertEquals(100, response.getMatchPercentage());
        assertTrue(response.isEligible());
    }

    @Test
    @DisplayName("Profile missing state and income yields PARTIALLY_ELIGIBLE or NOT_ELIGIBLE")
    void testPartialMatchEligibility() {
        User user = User.builder().fullName("Rajesh Patel").build();
        CitizenProfile profile = CitizenProfile.builder()
                .age(30)
                .gender("Male")
                .build();

        Scheme scheme = Scheme.builder()
                .schemeName("State Pension")
                .schemeType(SchemeType.STATE)
                .minimumAge(18)
                .maximumAge(60)
                .maximumIncome(new BigDecimal("200000"))
                .applicableStates(List.of("Tamil Nadu"))
                .allowedGenders(List.of("Female"))
                .build();

        EligibilityCheckResponse response = eligibilityEngine.evaluateEligibility(user, profile, scheme);

        assertTrue(response.getMatchPercentage() < 80);
    }
}
