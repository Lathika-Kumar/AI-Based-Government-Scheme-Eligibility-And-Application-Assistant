package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CitizenProfileRequest;
import com.schemebridge.scheme.dto.response.CitizenProfileResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenProfileServiceTest {

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @InjectMocks
    private CitizenProfileService citizenProfileService;

    private CitizenProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = CitizenProfile.builder()
                .id("prof-101")
                .userId("101")
                .displayName("Ramesh Kumar")
                .age(35)
                .gender("Male")
                .state("Maharashtra")
                .district("Pune")
                .annualIncome(150000.0)
                .occupation("Farmer")
                .socialCategory("OBC")
                .education("Graduate")
                .disabilityStatus(false)
                .onboardingComplete(true)
                .onboardingStep(3)
                .accessibilityPreferences(Map.of("fontSize", "large"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void testFindProfile_Found() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));

        Optional<CitizenProfile> result = citizenProfileService.findProfile("101");
        assertTrue(result.isPresent());
        assertEquals("Ramesh Kumar", result.get().getDisplayName());
        assertEquals(35, result.get().getAge());
    }

    @Test
    void testFindProfile_NotFound() {
        when(citizenProfileRepository.findByUserId("999")).thenReturn(Optional.empty());

        Optional<CitizenProfile> result = citizenProfileService.findProfile("999");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetOrCreateProfile_Existing() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));

        CitizenProfile result = citizenProfileService.getOrCreateProfile("101");
        assertNotNull(result);
        assertEquals("prof-101", result.getId());
        verify(citizenProfileRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateProfile_NewShell() {
        when(citizenProfileRepository.findByUserId("102")).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> {
            CitizenProfile p = invocation.getArgument(0);
            p.setId("prof-new");
            return p;
        });

        CitizenProfile result = citizenProfileService.getOrCreateProfile("102");
        assertNotNull(result);
        assertEquals("prof-new", result.getId());
        assertEquals("102", result.getUserId());
        assertFalse(result.getOnboardingComplete());
        assertEquals("NOT_STARTED", result.getOnboardingStatus());
        assertEquals(1, result.getOnboardingStep());
    }

    @Test
    void testGetOrCreateProfile_NormalizesLegacyEmptyProfile() {
        CitizenProfile legacyEmpty = CitizenProfile.builder()
                .userId("104")
                .onboardingComplete(false)
                .onboardingStep(0)
                .onboardingStatus(null)
                .build();

        when(citizenProfileRepository.findByUserId("104")).thenReturn(Optional.of(legacyEmpty));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfile result = citizenProfileService.getOrCreateProfile("104");
        assertNotNull(result);
        assertFalse(result.getOnboardingComplete());
        assertEquals("NOT_STARTED", result.getOnboardingStatus());
        assertEquals(1, result.getOnboardingStep());
    }

    @Test
    void testGetOrCreateProfile_NormalizesLegacyCompleteProfile() {
        CitizenProfile legacyComplete = CitizenProfile.builder()
                .userId("105")
                .displayName("Vikram Singh")
                .age(40)
                .gender("Male")
                .state("Punjab")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("General")
                .disabilityStatus(false)
                .onboardingComplete(true)
                .onboardingStep(3)
                .onboardingStatus(null)
                .build();

        when(citizenProfileRepository.findByUserId("105")).thenReturn(Optional.of(legacyComplete));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfile result = citizenProfileService.getOrCreateProfile("105");
        assertNotNull(result);
        assertTrue(result.getOnboardingComplete());
        assertEquals("COMPLETE", result.getOnboardingStatus());
        assertEquals(3, result.getOnboardingStep());
    }

    @Test
    void testIsOnboardingComplete() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));
        assertTrue(citizenProfileService.isOnboardingComplete("101"));

        CitizenProfile incomplete = CitizenProfile.builder().userId("102").onboardingComplete(false).build();
        when(citizenProfileRepository.findByUserId("102")).thenReturn(Optional.of(incomplete));
        assertFalse(citizenProfileService.isOnboardingComplete("102"));

        when(citizenProfileRepository.findByUserId("999")).thenReturn(Optional.empty());
        assertFalse(citizenProfileService.isOnboardingComplete("999"));
    }

    @Test
    void testUpsertProfile_New() {
        when(citizenProfileRepository.findByUserId("103")).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .displayName("Priya Sharma")
                .age(28)
                .gender("Female")
                .state("Karnataka")
                .annualIncome(200000.0)
                .occupation("Salaried Employee")
                .socialCategory("General")
                .disabilityStatus(false)
                .build();

        CitizenProfile saved = citizenProfileService.upsertProfile("103", req);
        assertNotNull(saved);
        assertEquals("103", saved.getUserId());
        assertEquals("Priya Sharma", saved.getDisplayName());
        assertEquals(28, saved.getAge());
        assertEquals("Female", saved.getGender());
        assertEquals("Karnataka", saved.getState());
        assertEquals(200000.0, saved.getAnnualIncome());
        assertTrue(saved.getOnboardingComplete());
        assertEquals("COMPLETE", saved.getOnboardingStatus());
        assertEquals(3, saved.getOnboardingStep());
    }

    @Test
    void testUpsertProfile_UpdateExistingPartial() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .annualIncome(180000.0)
                .occupation("Self-Employed")
                .build();

        CitizenProfile updated = citizenProfileService.upsertProfile("101", req);
        assertNotNull(updated);
        assertEquals(180000.0, updated.getAnnualIncome());
        assertEquals("Self-Employed", updated.getOccupation());
        // Existing fields preserved
        assertEquals("Ramesh Kumar", updated.getDisplayName());
        assertEquals("Maharashtra", updated.getState());
        assertEquals(35, updated.getAge());
    }

    @Test
    void testStateProgression_Step1Only_SetsStep2InProgress() {
        when(citizenProfileRepository.findByUserId("201")).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .displayName("Ananya Roy")
                .age(24)
                .gender("Female")
                .build();

        CitizenProfile saved = citizenProfileService.upsertProfile("201", req);
        assertNotNull(saved);
        assertFalse(saved.getOnboardingComplete());
        assertEquals("IN_PROGRESS", saved.getOnboardingStatus());
        assertEquals(2, saved.getOnboardingStep());
    }

    @Test
    void testStateProgression_Step2Only_SetsStep3InProgress() {
        CitizenProfile step1Profile = CitizenProfile.builder()
                .userId("202")
                .displayName("Ananya Roy")
                .age(24)
                .gender("Female")
                .onboardingStep(2)
                .onboardingStatus("IN_PROGRESS")
                .onboardingComplete(false)
                .build();

        when(citizenProfileRepository.findByUserId("202")).thenReturn(Optional.of(step1Profile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .occupation("Student")
                .annualIncome(0.0)
                .socialCategory("General")
                .state("West Bengal")
                .build();

        CitizenProfile saved = citizenProfileService.upsertProfile("202", req);
        assertNotNull(saved);
        assertFalse(saved.getOnboardingComplete());
        assertEquals("IN_PROGRESS", saved.getOnboardingStatus());
        assertEquals(3, saved.getOnboardingStep());
    }

    @Test
    void testStateProgression_Step3Complete_SetsComplete() {
        CitizenProfile step2Profile = CitizenProfile.builder()
                .userId("203")
                .displayName("Ananya Roy")
                .age(24)
                .gender("Female")
                .occupation("Student")
                .annualIncome(0.0)
                .socialCategory("General")
                .state("West Bengal")
                .onboardingStep(3)
                .onboardingStatus("IN_PROGRESS")
                .onboardingComplete(false)
                .build();

        when(citizenProfileRepository.findByUserId("203")).thenReturn(Optional.of(step2Profile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .disabilityStatus(false)
                .build();

        CitizenProfile saved = citizenProfileService.upsertProfile("203", req);
        assertNotNull(saved);
        assertTrue(saved.getOnboardingComplete());
        assertEquals("COMPLETE", saved.getOnboardingStatus());
        assertEquals(3, saved.getOnboardingStep());
    }

    @Test
    void testToCitizenEligibilityProfile() {
        CitizenEligibilityProfile eligibilityProfile = citizenProfileService.toCitizenEligibilityProfile(sampleProfile);
        assertNotNull(eligibilityProfile);
        assertEquals(35, eligibilityProfile.getAge());
        assertEquals("Male", eligibilityProfile.getGender());
        assertEquals("Maharashtra", eligibilityProfile.getState());
        assertEquals(150000.0, eligibilityProfile.getAnnualIncome());
        assertEquals("Farmer", eligibilityProfile.getOccupation());
        assertEquals("OBC", eligibilityProfile.getSocialCategory());
        assertFalse(eligibilityProfile.getDisabilityStatus());
    }

    @Test
    void testLinkVerifiedDocumentAttribute() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenProfile updated = citizenProfileService.linkVerifiedDocumentAttribute(
                "101",
                "annualIncome",
                120000.0,
                "OCR_EXTRACTED",
                "doc-999",
                "INCOME_CERTIFICATE",
                0.95
        );

        assertNotNull(updated);
        assertEquals(120000.0, updated.getAnnualIncome());
        assertNotNull(updated.getVerifiedAttributes());
        assertTrue(updated.getVerifiedAttributes().containsKey("annualIncome"));
        assertEquals(120000.0, updated.getVerifiedAttributes().get("annualIncome").getValue());
        assertTrue(updated.getVerifiedAttributes().get("annualIncome").getVerified());
        assertEquals("OCR_EXTRACTED", updated.getVerifiedAttributes().get("annualIncome").getSource());
        assertEquals("doc-999", updated.getVerifiedAttributes().get("annualIncome").getDocumentId());
        assertEquals(0.95, updated.getVerifiedAttributes().get("annualIncome").getConfidenceScore());
    }

    @Test
    void testUpsertProfile_WithDob_PersistsDobAndCalculatesAge() {
        when(citizenProfileRepository.findByUserId("301")).thenReturn(Optional.empty());
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate dob = LocalDate.of(2007, 3, 22);
        int expectedAge = java.time.Period.between(dob, LocalDate.now()).getYears();

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .displayName("Lathika Kumar")
                .dob(dob)
                .gender("Female")
                .build();

        CitizenProfile saved = citizenProfileService.upsertProfile("301", req);
        assertNotNull(saved);
        assertEquals("Lathika Kumar", saved.getDisplayName());
        assertEquals(dob, saved.getDob());
        assertEquals(expectedAge, saved.getAge());
    }

    @Test
    void testToResponse_IncludesDob() {
        LocalDate dob = LocalDate.of(2005, 8, 14);
        sampleProfile.setDob(dob);

        CitizenProfileResponse response = citizenProfileService.toResponse(sampleProfile);
        assertNotNull(response);
        assertEquals(dob, response.getDob());
        assertEquals(35, response.getAge());
    }

    @Test
    void testUpdateDob_UpdatesDobAndRecalculatesAge() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(sampleProfile));
        when(citizenProfileRepository.save(any(CitizenProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDob = LocalDate.of(2005, 8, 14);
        int expectedAge = java.time.Period.between(newDob, LocalDate.now()).getYears();

        CitizenProfileRequest req = CitizenProfileRequest.builder()
                .dob(newDob)
                .build();

        CitizenProfile updated = citizenProfileService.upsertProfile("101", req);
        assertNotNull(updated);
        assertEquals(newDob, updated.getDob());
        assertEquals(expectedAge, updated.getAge());
    }
}
