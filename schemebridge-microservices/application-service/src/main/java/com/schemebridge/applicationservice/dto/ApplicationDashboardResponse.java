package com.schemebridge.applicationservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDashboardResponse {
    private long totalApplications;
    private long approvedCount;
    private long pendingCount;
    private long rejectedCount;
    private long withdrawnCount;
    private long underReviewCount;
    private long benefitReleasedCount;
    private BigDecimal totalBenefitsReceived;
    private List<ApplicationSummaryResponse> recentApplications;
}
