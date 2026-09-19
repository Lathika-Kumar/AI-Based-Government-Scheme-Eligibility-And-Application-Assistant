package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetricsResponse {
    // Scheme metrics
    private long totalSchemes;
    private long activeSchemes;
    private long draftSchemes;
    private long inactiveSchemes;

    // Application metrics
    private long totalApplications;
    private long pendingApplications;
    private long underReviewApplications;
    private long correctionRequiredApplications;
    private long approvedApplications;
    private long rejectedApplications;
    private long cancelledApplications;

    // Document metrics
    private long documentsPending;
    private long documentsVerified;
    private long documentsRejected;

    // Grievance & Feedback metrics
    private long totalGrievances;
    private long openGrievances;
    private long inProgressGrievances;
    private long resolvedGrievances;
    private long overdueGrievances;
    private long totalFeedback;

    // Time-based application submission counts
    private long applicationsToday;
    private long applicationsThisWeek;
    private long applicationsThisMonth;

    // Notification metrics
    private long unreadAdminNotifications;

    // Visual chart & trend breakdowns
    private Map<String, Long> applicationsByStatus;
    private Map<String, Long> applicationsByScheme;
    private Map<String, Long> applicationsByState;
    private Map<String, Long> monthlyTrend;
}
