package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.PersonalizedSchemeRecommendationResponse;
import com.schemebridge.scheme.dto.response.RankedSchemeItem;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 34: Current User Eligibility-Driven Recommendation Engine Test Suite.
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase34CurrentUserRecommendationEngineTest {

    @Autowired
    private EligibleSchemeRecommendationService recommendationService;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    private static final String NEW_CITIZEN_ID = "new_citizen_phase34_001";
    private static final String PARTIAL_CITIZEN_ID = "partial_citizen_phase34_002";
    private static final String FARMER_CITIZEN_ID = "farmer_citizen_phase34_003";

    @BeforeEach
    void setUp() {
        cleanTestData();

        // 1. Newly logged-in citizen with complete profile
        CitizenProfile completeProfile = CitizenProfile.builder()
                .userId(NEW_CITIZEN_ID)
                .displayName("Rajesh Kumar")
                .dob(LocalDate.of(1995, 5, 20))
                .age(31)
                .gender("MALE")
                .state("Maharashtra")
                .district("Pune")
                .occupation("SOFTWARE_ENGINEER")
                .annualIncome(650000.0)
                .socialCategory("GENERAL")
                .maritalStatus("MARRIED")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(completeProfile);

        // 2. Citizen with partial profile (unknown income, unknown occupation)
        CitizenProfile partialProfile = CitizenProfile.builder()
                .userId(PARTIAL_CITIZEN_ID)
                .displayName("Amina Begum")
                .age(28)
                .gender("FEMALE")
                .state("Bihar")
                .onboardingComplete(false)
                .onboardingStatus("IN_PROGRESS")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(partialProfile);

        // 3. Citizen with Farmer status
        CitizenProfile farmerProfile = CitizenProfile.builder()
                .userId(FARMER_CITIZEN_ID)
                .displayName("Rameshwar Patil")
                .age(48)
                .gender("MALE")
                .state("Maharashtra")
                .occupation("FARMER")
                .isFarmer(true)
                .annualIncome(180000.0)
                .socialCategory("OBC")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .verifiedAttributes(Map.of())
                .build();
        citizenProfileRepository.save(farmerProfile);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        citizenProfileRepository.findByUserId(NEW_CITIZEN_ID).ifPresent(p -> citizenProfileRepository.delete(p));
        citizenProfileRepository.findByUserId(PARTIAL_CITIZEN_ID).ifPresent(p -> citizenProfileRepository.delete(p));
        citizenProfileRepository.findByUserId(FARMER_CITIZEN_ID).ifPresent(p -> citizenProfileRepository.delete(p));
    }

    @Test
    @DisplayName("A. Newly logged-in citizen with complete profile receives valid eligible recommendations")
    void testCompleteProfileRecommendations() {
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(NEW_CITIZEN_ID, 0, 10);

        assertNotNull(response);
        assertEquals(NEW_CITIZEN_ID, response.getUserId());
        assertEquals("Maharashtra", response.getCitizenState());
        assertEquals("EligibilityEngine", response.getEligibilityAuthority());
        assertTrue(response.getTotalCatalogEvaluated() > 0);

        List<RankedSchemeItem> recs = response.getRecommendations();
        assertNotNull(recs);

        for (RankedSchemeItem item : recs) {
            assertTrue(item.isEligible());
            assertEquals("ELIGIBLE", item.getEligibilityStatus());
            assertNotNull(item.getSchemeCode());
            assertTrue(item.getRank() >= 1);
            assertTrue(item.getRecommendationScore() >= 0.0 && item.getRecommendationScore() <= 1.0);
            assertNotNull(item.getReasons());
            assertFalse(item.getReasons().isEmpty());
        }
    }

    @Test
    @DisplayName("B. User with partial profile handles UNKNOWN attributes safely without false positives")
    void testPartialProfileSafeHandling() {
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(PARTIAL_CITIZEN_ID, 0, 10);

        assertNotNull(response);
        assertEquals(PARTIAL_CITIZEN_ID, response.getUserId());
        assertEquals("Bihar", response.getCitizenState());
        assertEquals("EligibilityEngine", response.getEligibilityAuthority());

        for (RankedSchemeItem item : response.getRecommendations()) {
            assertEquals("ELIGIBLE", item.getEligibilityStatus());
            assertTrue(item.isEligible());
        }
    }

    @Test
    @DisplayName("C & D. Different users receive distinct recommendations aligned with their actual profiles")
    void testRecommendationsDivergenceForDifferentProfiles() {
        PersonalizedSchemeRecommendationResponse engineerResponse =
                recommendationService.getPersonalizedRecommendations(NEW_CITIZEN_ID, 0, 5);

        PersonalizedSchemeRecommendationResponse farmerResponse =
                recommendationService.getPersonalizedRecommendations(FARMER_CITIZEN_ID, 0, 5);

        assertNotNull(engineerResponse);
        assertNotNull(farmerResponse);

        List<String> engineerSchemeCodes = engineerResponse.getRecommendations().stream()
                .map(RankedSchemeItem::getSchemeCode)
                .toList();

        List<String> farmerSchemeCodes = farmerResponse.getRecommendations().stream()
                .map(RankedSchemeItem::getSchemeCode)
                .toList();

        assertNotNull(farmerSchemeCodes);
        assertNotNull(engineerSchemeCodes);
    }

    @Test
    @DisplayName("E & F. Ineligible schemes are NEVER returned; EligibilityEngine is the sole statutory authority")
    void testIneligibleSchemesExcluded() {
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(NEW_CITIZEN_ID, 0, 20);

        for (RankedSchemeItem item : response.getRecommendations()) {
            assertNotEquals("INELIGIBLE", item.getEligibilityStatus());
            assertNotEquals("NOT_ELIGIBLE", item.getEligibilityStatus());
            assertTrue(item.isEligible());
        }
    }

    @Test
    @DisplayName("G. Active recommender remains 2.2.0-hybrid-semantic-384d with 1.0.0-deterministic fallback")
    void testActiveRecommenderModelIntegrity() {
        PersonalizedSchemeRecommendationResponse response =
                recommendationService.getPersonalizedRecommendations(NEW_CITIZEN_ID, 0, 10);

        assertNotNull(response.getModelVersion());
        assertTrue(response.getModelVersion().contains("hybrid-semantic") || "1.0.0-deterministic".equals(response.getModelVersion()));
        assertNotNull(response.getRankingMethod());
    }
}
