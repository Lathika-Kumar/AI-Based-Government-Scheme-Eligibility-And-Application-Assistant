package com.schemebridge.dto;

import com.schemebridge.enums.ProfileCompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private String fullName;
    private ProfileCompletionStatus profileStatus;
    private int profileCompletionPercentage;
    private long eligibleSchemesCount;
    private long partiallyEligibleSchemesCount;
    private long completedApplicationsCount;
    private long pendingApplicationsCount;
    private int documentCompletionPercentage;
    private long unreadNotificationsCount;
    private boolean onboardingCompleted;
}
