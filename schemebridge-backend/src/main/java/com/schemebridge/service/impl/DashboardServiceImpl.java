package com.schemebridge.service.impl;

import com.schemebridge.dto.DashboardSummaryResponse;
import com.schemebridge.dto.ProfileCompletionResponse;
import com.schemebridge.dto.SchemeRecommendationResponse;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.User;
import com.schemebridge.enums.EligibilityResult;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.repository.CitizenProfileRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.DashboardService;
import com.schemebridge.service.SchemeService;
import com.schemebridge.util.ProfileCompletionCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final ProfileCompletionCalculator completionCalculator;
    private final SchemeService schemeService;

    @Override
    public DashboardSummaryResponse getDashboardSummary(String userEmail) {
        log.info("Fetching lightweight dashboard summary metrics for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        CitizenProfile profile = citizenProfileRepository.findByUserId(user.getId())
                .orElse(null);

        ProfileCompletionResponse completion = completionCalculator.calculateCompletion(user, profile);

        List<SchemeRecommendationResponse> recommendations = schemeService.getRecommendations(userEmail);

        long eligibleCount = recommendations.stream()
                .filter(r -> r.getEligibilityResult() == EligibilityResult.ELIGIBLE)
                .count();

        long partiallyEligibleCount = recommendations.stream()
                .filter(r -> r.getEligibilityResult() == EligibilityResult.PARTIALLY_ELIGIBLE)
                .count();

        // Calculate document completion %
        int docScore = 0;
        if (profile != null) {
            if (profile.getAadhaarNumber() != null && !profile.getAadhaarNumber().isBlank()) docScore += 50;
            if (profile.getPanNumber() != null && !profile.getPanNumber().isBlank()) docScore += 50;
        }

        return DashboardSummaryResponse.builder()
                .fullName(user.getFullName())
                .profileStatus(completion.getStatus())
                .profileCompletionPercentage(completion.getCompletionPercentage())
                .eligibleSchemesCount(eligibleCount)
                .partiallyEligibleSchemesCount(partiallyEligibleCount)
                .completedApplicationsCount(0L) // Default 0 for Phase 3
                .pendingApplicationsCount(0L)   // Default 0 for Phase 3
                .documentCompletionPercentage(docScore)
                .unreadNotificationsCount(0L)  // Default 0 for Phase 3
                .onboardingCompleted(Boolean.TRUE.equals(user.getOnboardingCompleted()))
                .build();
    }
}
