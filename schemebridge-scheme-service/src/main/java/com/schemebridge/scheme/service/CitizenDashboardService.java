package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.CitizenDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitizenDashboardService {

    private final CitizenProfileService citizenProfileService;
    private final SchemeService schemeService;
    private final ApplicationService applicationService;

    public CitizenDashboardResponse getCitizenDashboardSummary(String userId) {
        CitizenProfile profile = citizenProfileService.getOrCreateProfile(userId);
        CitizenEligibilityProfile eligibilityProfile = citizenProfileService.toCitizenEligibilityProfile(profile);

        // 1. Real Eligibility Count
        int eligibleSchemesCount = 0;
        try {
            BulkEligibilityEvaluationResponse evalRes = schemeService.evaluateEligibilityForAll(eligibilityProfile, SchemeStatus.ACTIVE);
            if (evalRes != null && evalRes.getEligible() != null) {
                eligibleSchemesCount = evalRes.getEligible().size();
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate eligibility count for userId={}: {}", userId, e.getMessage());
        }

        // 2. Real Applications Count
        int completedApplications = 0;
        int pendingApplications = 0;
        try {
            List<ApplicationResponse> apps = applicationService.getMyApplications(userId);
            if (apps != null) {
                for (ApplicationResponse app : apps) {
                    String status = app.getStatus() != null ? app.getStatus().toUpperCase() : "";
                    if ("APPROVED".equals(status) || "DISBURSED".equals(status)) {
                        completedApplications++;
                    } else if ("SUBMITTED".equals(status) || "UNDER_REVIEW".equals(status) || "DRAFT".equals(status) || "ACTION_REQUIRED".equals(status)) {
                        pendingApplications++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch applications for userId={}: {}", userId, e.getMessage());
        }

        // 3. Profile Completion Score
        int profileCompletionPercentage = calculateProfileCompletion(profile);

        // 4. Document Completion Percentage
        int documentCompletionPercentage = Boolean.TRUE.equals(profile.getOnboardingComplete()) ? 100 : (profile.getOnboardingStep() != null ? profile.getOnboardingStep() * 33 : 0);

        // 5. Action Items
        List<String> actionItems = new ArrayList<>();
        if (eligibleSchemesCount > 0) {
            actionItems.add(String.format("You qualify for %d active government welfare schemes.", eligibleSchemesCount));
        }
        if (pendingApplications > 0) {
            actionItems.add(String.format("You have %d application%s currently in progress.", pendingApplications, pendingApplications > 1 ? "s" : ""));
        }
        if (profileCompletionPercentage < 100) {
            actionItems.add(String.format("Complete the remaining %d%% of your profile to discover targeted benefits.", 100 - profileCompletionPercentage));
        }

        // 6. Action Brief summary
        String aiSummary = String.format("You qualify for %d welfare schemes with %d active applications. Profile completion is %d%%.",
                eligibleSchemesCount, pendingApplications, profileCompletionPercentage);

        return CitizenDashboardResponse.builder()
                .userId(userId)
                .eligibleSchemesCount(eligibleSchemesCount)
                .completedApplicationsCount(completedApplications)
                .pendingApplicationsCount(pendingApplications)
                .documentCompletionPercentage(documentCompletionPercentage)
                .profileCompletionPercentage(profileCompletionPercentage)
                .actionItems(actionItems)
                .aiSummary(aiSummary)
                .generatedAt(Instant.now())
                .build();
    }

    private int calculateProfileCompletion(CitizenProfile p) {
        if (p == null) return 0;
        int total = 7;
        int count = 0;
        if (p.getDisplayName() != null && !p.getDisplayName().isBlank()) count++;
        if (p.getAge() != null && p.getAge() > 0) count++;
        if (p.getGender() != null && !p.getGender().isBlank()) count++;
        if (p.getState() != null && !p.getState().isBlank()) count++;
        if (p.getOccupation() != null && !p.getOccupation().isBlank()) count++;
        if (p.getAnnualIncome() != null && p.getAnnualIncome() >= 0) count++;
        if (p.getSocialCategory() != null && !p.getSocialCategory().isBlank()) count++;
        return Math.round(((float) count / total) * 100);
    }
}
