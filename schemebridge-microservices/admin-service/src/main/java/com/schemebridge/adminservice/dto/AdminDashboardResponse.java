package com.schemebridge.adminservice.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    private Long totalCitizens;
    private Long activeCitizens;
    private Long totalOfficers;
    private Long activeOfficers;

    private Long totalSchemes;
    private Long publishedSchemes;

    private Long totalApplications;
    private Long pendingApplications;
    private Long approvedApplications;
    private Long rejectedApplications;

    private Long totalDocuments;
    private Long pendingDocumentVerifications;

    private Long totalFeedbackSubmitted;
    private Long pendingFeedbackCount;

    private Map<String, Long> applicationsByStatus;
    private Map<String, Long> officersByRole;
    private List<AdminActivityLogResponse> recentActivities;
}
