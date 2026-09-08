package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.CitizenDashboardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenDashboardServiceTest {

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private SchemeService schemeService;

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private CitizenDashboardService citizenDashboardService;

    private CitizenProfile mockProfile;
    private CitizenEligibilityProfile mockEligibilityProfile;

    @BeforeEach
    void setUp() {
        mockProfile = CitizenProfile.builder()
                .userId("101")
                .displayName("Aarav Kumar")
                .age(28)
                .gender("Male")
                .state("Maharashtra")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("OBC")
                .onboardingComplete(true)
                .onboardingStep(3)
                .onboardingStatus("COMPLETE")
                .build();

        mockEligibilityProfile = CitizenEligibilityProfile.builder()
                .age(28)
                .gender("Male")
                .state("Maharashtra")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("OBC")
                .build();
    }

    @Test
    void testGetCitizenDashboardSummary_Success() {
        when(citizenProfileService.getOrCreateProfile("101")).thenReturn(mockProfile);
        when(citizenProfileService.toCitizenEligibilityProfile(mockProfile)).thenReturn(mockEligibilityProfile);

        BulkEligibilityEvaluationResponse bulkEval = BulkEligibilityEvaluationResponse.builder()
                .eligible(List.of(
                        com.schemebridge.scheme.dto.response.SchemeEvaluationSummary.builder().schemeCode("SCH-1").build(),
                        com.schemebridge.scheme.dto.response.SchemeEvaluationSummary.builder().schemeCode("SCH-2").build(),
                        com.schemebridge.scheme.dto.response.SchemeEvaluationSummary.builder().schemeCode("SCH-3").build(),
                        com.schemebridge.scheme.dto.response.SchemeEvaluationSummary.builder().schemeCode("SCH-4").build(),
                        com.schemebridge.scheme.dto.response.SchemeEvaluationSummary.builder().schemeCode("SCH-5").build()
                ))
                .build();
        when(schemeService.evaluateEligibilityForAll(eq(mockEligibilityProfile), eq(SchemeStatus.ACTIVE)))
                .thenReturn(bulkEval);

        List<ApplicationResponse> apps = List.of(
                ApplicationResponse.builder().id("app-1").status("APPROVED").build(),
                ApplicationResponse.builder().id("app-2").status("SUBMITTED").build(),
                ApplicationResponse.builder().id("app-3").status("UNDER_REVIEW").build()
        );
        when(applicationService.getMyApplications("101")).thenReturn(apps);

        CitizenDashboardResponse res = citizenDashboardService.getCitizenDashboardSummary("101");

        assertNotNull(res);
        assertEquals("101", res.getUserId());
        assertEquals(5, res.getEligibleSchemesCount());
        assertEquals(1, res.getCompletedApplicationsCount());
        assertEquals(2, res.getPendingApplicationsCount());
        assertEquals(100, res.getProfileCompletionPercentage());
        assertEquals(100, res.getDocumentCompletionPercentage());
        assertNotNull(res.getActionItems());
        assertTrue(res.getActionItems().size() >= 2);
        assertNotNull(res.getAiSummary());
    }

    @Test
    void testGetCitizenDashboardSummary_EmptyProfile() {
        CitizenProfile emptyProfile = CitizenProfile.builder()
                .userId("102")
                .onboardingComplete(false)
                .onboardingStep(1)
                .onboardingStatus("NOT_STARTED")
                .build();

        when(citizenProfileService.getOrCreateProfile("102")).thenReturn(emptyProfile);
        when(citizenProfileService.toCitizenEligibilityProfile(emptyProfile)).thenReturn(CitizenEligibilityProfile.builder().build());

        CitizenDashboardResponse res = citizenDashboardService.getCitizenDashboardSummary("102");

        assertNotNull(res);
        assertEquals("102", res.getUserId());
        assertEquals(0, res.getEligibleSchemesCount());
        assertEquals(0, res.getCompletedApplicationsCount());
        assertEquals(0, res.getPendingApplicationsCount());
        assertEquals(0, res.getProfileCompletionPercentage());
        assertNotNull(res.getAiSummary());
    }

    @Test
    void testGetCitizenDashboardSummary_DownstreamFailure_GracefulFallback() {
        when(citizenProfileService.getOrCreateProfile("103")).thenReturn(mockProfile);
        when(citizenProfileService.toCitizenEligibilityProfile(mockProfile)).thenReturn(mockEligibilityProfile);

        when(schemeService.evaluateEligibilityForAll(any(), any()))
                .thenThrow(new RuntimeException("Downstream timeout"));
        when(applicationService.getMyApplications("103"))
                .thenThrow(new RuntimeException("Application store unavailable"));

        CitizenDashboardResponse res = citizenDashboardService.getCitizenDashboardSummary("103");

        assertNotNull(res);
        assertEquals("103", res.getUserId());
        assertEquals(0, res.getEligibleSchemesCount());
        assertEquals(0, res.getCompletedApplicationsCount());
        assertEquals(0, res.getPendingApplicationsCount());
        assertNotNull(res.getAiSummary());
    }
}
