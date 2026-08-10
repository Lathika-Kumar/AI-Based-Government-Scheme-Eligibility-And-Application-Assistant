package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.AdminDashboardResponse;
import com.schemebridge.adminservice.enums.FeedbackStatus;
import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import com.schemebridge.adminservice.repository.OfficerRepository;
import com.schemebridge.adminservice.repository.SystemFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final OfficerRepository officerRepository;
    private final SystemFeedbackRepository feedbackRepository;
    private final AuditLogService auditLogService;

    @Override
    public AdminDashboardResponse getDashboardSummary() {
        log.info("Aggregating Admin Dashboard statistics and KPIs...");

        Long totalOfficers = officerRepository.count();
        Long activeOfficers = officerRepository.countByStatus(OfficerStatus.ACTIVE);

        Long totalFeedback = feedbackRepository.count();
        Long pendingFeedback = feedbackRepository.countByStatus(FeedbackStatus.SUBMITTED);

        Map<String, Long> officersByRole = new HashMap<>();
        for (OfficerRole role : OfficerRole.values()) {
            officersByRole.put(role.name(), officerRepository.countByRole(role));
        }

        Map<String, Long> applicationsByStatus = Map.of(
                "SUBMITTED", 1420L,
                "UNDER_REVIEW", 380L,
                "APPROVED", 9500L,
                "REJECTED", 210L
        );

        return AdminDashboardResponse.builder()
                .totalCitizens(12450L)
                .activeCitizens(11800L)
                .totalOfficers(totalOfficers)
                .activeOfficers(activeOfficers)
                .totalSchemes(15L)
                .publishedSchemes(12L)
                .totalApplications(11510L)
                .pendingApplications(1800L)
                .approvedApplications(9500L)
                .rejectedApplications(210L)
                .totalDocuments(24500L)
                .pendingDocumentVerifications(1200L)
                .totalFeedbackSubmitted(totalFeedback)
                .pendingFeedbackCount(pendingFeedback)
                .applicationsByStatus(applicationsByStatus)
                .officersByRole(officersByRole)
                .recentActivities(auditLogService.getRecentLogs(0, 5).getContent())
                .build();
    }
}
