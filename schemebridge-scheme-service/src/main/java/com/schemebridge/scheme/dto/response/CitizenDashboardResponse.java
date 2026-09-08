package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenDashboardResponse {
    private String userId;
    private int eligibleSchemesCount;
    private int completedApplicationsCount;
    private int pendingApplicationsCount;
    private int documentCompletionPercentage;
    private int profileCompletionPercentage;
    private List<String> actionItems;
    private String aiSummary;
    private Instant generatedAt;
}
