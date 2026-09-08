package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformAdminEndToEndTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private ApplicationReviewRepository applicationReviewRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private EligibilityEngine eligibilityEngine;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private MongoOperations mongoOperations;

    @Mock
    private ApplicationEventService applicationEventService;

    @Mock
    private ApplicationStatusTransitionService applicationStatusTransitionService;

    @Mock
    private DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private ApplicationService applicationService;

    private ApplicationReviewService applicationReviewService;

    private Scheme testScheme;
    private Application testApplication;
    private ApplicationDocument testDoc;

    @BeforeEach
    void setUp() {
        applicationReviewService = new ApplicationReviewService(
                applicationRepository,
                applicationDocumentRepository,
                applicationReviewRepository,
                schemeRepository,
                applicationEventService,
                applicationStatusTransitionService,
                detailedDocumentStatusTransitionService,
                applicationService,
                mongoOperations,
                notificationService,
                adminAuditService
        );
        lenient().when(detailedDocumentStatusTransitionService.transitionDocumentStatus(any(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        RequiredDocument rd = RequiredDocument.builder()
                .documentCode("INCOME_PROOF")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG", "PNG"))
                .build();

        testScheme = Scheme.builder()
                .id("scheme-100")
                .schemeCode("PMAY-U")
                .title(MultilingualText.builder().english("Pradhan Mantri Awas Yojana").build())
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of(rd))
                .build();


        testApplication = Application.builder()
                .id("app-100")
                .applicationNumber("SB-APP-2026-000100")
                .userId("citizen-1")
                .schemeId("scheme-100")
                .schemeCode("PMAY-U")
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testDoc = ApplicationDocument.builder()
                .id("doc-100")
                .applicationId("app-100")
                .userId("citizen-1")
                .documentCode("INCOME_PROOF")
                .documentName("Income Certificate")
                .mandatory(true)
                .uploaded(false)
                .version(0)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();
    }


    @Test
    void testCompleteCitizenToAdminWorkflow() {
        // Step 1: Upload Document
        when(applicationRepository.findById("app-100")).thenReturn(Optional.of(testApplication));
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode("app-100", "INCOME_PROOF")).thenReturn(Optional.of(testDoc));
        when(schemeRepository.findBySchemeCode("PMAY-U")).thenReturn(Optional.of(testScheme));
        when(documentStorageService.store(eq("app-100"), eq("INCOME_PROOF"), any())).thenReturn("gridfs-file-id-123");
        when(applicationDocumentRepository.findAllByApplicationId("app-100")).thenReturn(List.of(testDoc));

        lenient().when(applicationStatusTransitionService.isValidTransition(any(), any())).thenReturn(true);
        when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "income.pdf", "application/pdf", "dummy pdf content".getBytes());

        ApplicationDocumentResponse uploadRes = applicationService.uploadDocument("app-100", "INCOME_PROOF", file, "citizen-1");

        assertNotNull(uploadRes);
        assertTrue(uploadRes.isUploaded());
        assertEquals("INCOME_PROOF", uploadRes.getDocumentCode());

        // Step 2: Citizen Submits Application
        ApplicationResponse submitRes = applicationService.submitApplication("app-100", "citizen-1");

        assertNotNull(submitRes);
        assertEquals("SUBMITTED", submitRes.getStatus());
        verify(notificationService).sendNotification(any(), eq("ROLE_ADMIN"), eq(NotificationType.APPLICATION_SUBMITTED), any(), any(), any(), any(), eq("app-100"), eq("citizen-1"), any());

        // Step 3: Admin Starts Review
        testApplication.setStatus(ApplicationStatus.SUBMITTED);
        when(applicationStatusTransitionService.isValidTransition(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW)).thenReturn(true);

        ApplicationResponse reviewStartRes = applicationReviewService.startReview("app-100", "admin-1", "ROLE_ADMIN");
        assertNotNull(reviewStartRes);
        assertEquals("UNDER_REVIEW", reviewStartRes.getStatus());
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.APPLICATION_UNDER_REVIEW), any(), any(), any(), any(), eq("app-100"), eq("admin-1"), any());
        verify(adminAuditService).recordAction(eq("admin-1"), eq("ROLE_ADMIN"), eq("APPLICATION_REVIEW_STARTED"), eq("APPLICATION"), eq("app-100"), any(), any(), any(), any(), any());

        // Step 4: Admin Rejects Document with Reason
        testApplication.setStatus(ApplicationStatus.UNDER_REVIEW);
        testDoc.setUploaded(true);
        ApplicationResponse rejectDocRes = applicationReviewService.rejectDocument("app-100", "INCOME_PROOF", "Blurry income certificate scan", "admin-1");
        assertNotNull(rejectDocRes);
        assertEquals("CORRECTION_REQUIRED", rejectDocRes.getStatus());
        assertEquals(DocumentVerificationStatus.REJECTED, testDoc.getVerificationStatus());
        assertEquals("Blurry income certificate scan", testDoc.getRejectionReason());
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.CORRECTION_REQUIRED), any(), any(), any(), any(), eq("app-100"), eq("admin-1"), any());

        // Step 5: Citizen Re-uploads Corrected Document
        MockMultipartFile correctedFile = new MockMultipartFile("file", "income_clear.pdf", "application/pdf", "clear pdf".getBytes());
        ApplicationDocumentResponse correctedRes = applicationService.uploadDocument("app-100", "INCOME_PROOF", correctedFile, "citizen-1");
        assertNotNull(correctedRes);
        assertEquals(2, correctedRes.getVersion());
        assertEquals("PENDING", correctedRes.getVerificationStatus());

        // Step 6: Admin Verifies Document
        testApplication.setStatus(ApplicationStatus.UNDER_REVIEW);
        ApplicationResponse verifiedDocRes = applicationReviewService.verifyDocument("app-100", "INCOME_PROOF", "admin-1");
        assertNotNull(verifiedDocRes);
        assertEquals(DocumentVerificationStatus.VERIFIED, testDoc.getVerificationStatus());
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.DOCUMENT_VERIFIED), any(), any(), any(), any(), eq("app-100"), eq("admin-1"), any());

        // Step 7: Admin Approves Application
        when(applicationStatusTransitionService.isValidTransition(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)).thenReturn(true);
        ApplicationResponse approvedRes = applicationReviewService.approveApplication("app-100", "All criteria met.", "admin-1", "ROLE_ADMIN");
        assertNotNull(approvedRes);
        assertEquals("APPROVED", approvedRes.getStatus());
        verify(notificationService).sendNotification(eq("citizen-1"), any(), eq(NotificationType.APPLICATION_APPROVED), any(), any(), any(), any(), eq("app-100"), eq("admin-1"), any());
        verify(adminAuditService).recordAction(eq("admin-1"), eq("ROLE_ADMIN"), eq("APPLICATION_APPROVED"), eq("APPLICATION"), eq("app-100"), any(), any(), any(), any(), any());
    }

    @Test
    void testCitizenUnauthorizedAccess_ThrowsSecurityException() {
        when(applicationRepository.findById("app-100")).thenReturn(Optional.of(testApplication));

        assertThrows(SecurityException.class, () ->
                applicationService.submitApplication("app-100", "malicious-user"));
    }
}
