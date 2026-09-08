package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.UpdateAdminSettingsRequest;
import com.schemebridge.scheme.dto.response.AdminMetricsResponse;
import com.schemebridge.scheme.dto.response.AdminSettingsResponse;
import com.schemebridge.scheme.dto.response.PagedAdminAuditLogResponse;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOperationsTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AdminSettingsRepository adminSettingsRepository;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private AdminMetricsService adminMetricsService;
    private AdminSettingsService adminSettingsService;
    private AdminAuditService adminAuditService;
    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        adminAuditService = new AdminAuditService(adminAuditLogRepository, mongoTemplate);
        adminSettingsService = new AdminSettingsService(adminSettingsRepository, adminAuditService);
        adminMetricsService = new AdminMetricsService(schemeRepository, applicationRepository, applicationDocumentRepository, grievanceRepository, notificationRepository, mongoTemplate);
        adminReportService = new AdminReportService(mongoTemplate, applicationRepository, schemeRepository, grievanceRepository);
    }


    @Test
    void testGetMetrics_Success() {
        when(schemeRepository.count()).thenReturn(10L);
        when(schemeRepository.countByStatus(SchemeStatus.ACTIVE)).thenReturn(8L);
        when(schemeRepository.countByStatus(SchemeStatus.DRAFT)).thenReturn(1L);
        when(schemeRepository.countByStatus(SchemeStatus.INACTIVE)).thenReturn(1L);
        when(schemeRepository.countByStatus(SchemeStatus.ARCHIVED)).thenReturn(0L);

        when(applicationRepository.count()).thenReturn(25L);
        when(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)).thenReturn(5L);
        when(applicationRepository.countByStatus(ApplicationStatus.DOCUMENTS_PENDING)).thenReturn(3L);
        when(applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW)).thenReturn(4L);
        when(applicationRepository.countByStatus(ApplicationStatus.CORRECTION_REQUIRED)).thenReturn(2L);
        when(applicationRepository.countByStatus(ApplicationStatus.APPROVED)).thenReturn(10L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(1L);
        when(applicationRepository.countByStatus(ApplicationStatus.CANCELLED)).thenReturn(0L);

        when(applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.PENDING)).thenReturn(6L);
        when(applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.VERIFIED)).thenReturn(18L);
        when(applicationDocumentRepository.countByVerificationStatus(DocumentVerificationStatus.REJECTED)).thenReturn(2L);

        when(grievanceRepository.count()).thenReturn(5L);
        when(grievanceRepository.countByStatus(GrievanceStatus.OPEN)).thenReturn(2L);
        when(grievanceRepository.countByStatus(GrievanceStatus.IN_PROGRESS)).thenReturn(1L);
        when(grievanceRepository.countByStatus(GrievanceStatus.WAITING_FOR_CITIZEN)).thenReturn(0L);
        when(grievanceRepository.countByStatus(GrievanceStatus.RESOLVED)).thenReturn(2L);
        when(grievanceRepository.countByStatus(GrievanceStatus.CLOSED)).thenReturn(0L);

        AdminMetricsResponse metrics = adminMetricsService.getMetrics();
        assertNotNull(metrics);
        assertEquals(10L, metrics.getTotalSchemes());
        assertEquals(8L, metrics.getActiveSchemes());
        assertEquals(25L, metrics.getTotalApplications());
        assertEquals(8L, metrics.getPendingApplications());
        assertEquals(4L, metrics.getUnderReviewApplications());
        assertEquals(10L, metrics.getApprovedApplications());
        assertEquals(5L, metrics.getTotalGrievances());
    }

    @Test
    void testGetSettings_DefaultCreated() {
        when(adminSettingsRepository.findById("DEFAULT_SETTINGS")).thenReturn(Optional.empty());
        when(adminSettingsRepository.save(any(AdminSettings.class))).thenAnswer(i -> i.getArgument(0));

        AdminSettingsResponse settings = adminSettingsService.getSettings();
        assertNotNull(settings);
        assertEquals(7, settings.getApplicationSlaDays());
        assertTrue(settings.isEmailNotificationsEnabled());
    }

    @Test
    void testUpdateSettings_Success() {
        AdminSettings current = AdminSettings.builder().id("DEFAULT_SETTINGS").applicationSlaDays(7).build();
        when(adminSettingsRepository.findById("DEFAULT_SETTINGS")).thenReturn(Optional.of(current));
        when(adminSettingsRepository.save(any(AdminSettings.class))).thenAnswer(i -> i.getArgument(0));

        UpdateAdminSettingsRequest req = UpdateAdminSettingsRequest.builder()
                .applicationSlaDays(10)
                .emailNotificationsEnabled(false)
                .build();

        AdminSettingsResponse response = adminSettingsService.updateSettings(req, "admin-1", "ROLE_ADMIN");
        assertNotNull(response);
        assertEquals(10, response.getApplicationSlaDays());
        assertFalse(response.isEmailNotificationsEnabled());
    }

    @Test
    void testGenerateApplicationsCsv() {
        Application app = Application.builder()
                .applicationNumber("SB-APP-2026-000001")
                .userId("citizen-1")
                .schemeCode("PMAY-U")
                .status(ApplicationStatus.APPROVED)
                .createdAt(Instant.now())
                .build();

        when(mongoTemplate.find(any(Query.class), eq(Application.class))).thenReturn(List.of(app));

        String csv = adminReportService.generateApplicationsCsv("APPROVED", null);
        assertNotNull(csv);
        assertTrue(csv.contains("SB-APP-2026-000001"));
        assertTrue(csv.contains("PMAY-U"));
        assertTrue(csv.contains("APPROVED"));
    }
}
