package com.schemebridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeUsers;
    private long verifiedUsers;
    private long pendingUsers;
    private long totalSchemes;
    private long publishedSchemes;
    private long draftSchemes;
    private long archivedSchemes;
    private long applicationsSubmitted;
    private long applicationsApproved;
    private long applicationsRejected;
    private long applicationsPending;
    private long documentsUploaded;
    private long verifiedDocuments;
    private long pendingDocuments;
}
