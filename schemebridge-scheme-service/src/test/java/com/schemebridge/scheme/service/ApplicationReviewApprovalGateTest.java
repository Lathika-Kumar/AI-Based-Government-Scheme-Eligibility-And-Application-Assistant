package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationReviewApprovalGateTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private ApplicationReviewRepository applicationReviewRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private ApplicationEventService applicationEventService;

    @Mock
    private ApplicationStatusTransitionService applicationStatusTransitionService;

    @Mock
    private DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private MongoOperations mongoOperations;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminAuditService adminAuditService;

    private ApplicationReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ApplicationReviewService(
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

        lenient().when(applicationStatusTransitionService.isValidTransition(any(), any())).thenReturn(true);
        lenient().when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(applicationService.mapToResponse(any(Application.class))).thenReturn(new ApplicationResponse());
    }

    @Test
    @DisplayName("1. Approval Gated: Rejects approval when mandatory document is only AI_VERIFIED")
    void testApproveApplication_WhenMandatoryDocIsOnlyAiVerified_ThrowsException() {
        Application app = Application.builder()
                .id("app-gate-1")
                .applicationNumber("SB-GATE-001")
                .userId("citizen-10")
                .schemeCode("SCHEME_A")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findById("app-gate-1")).thenReturn(Optional.of(app));

        // Mandatory doc has passed AI verification, but is NOT yet officer-verified
        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-1")
                .applicationId("app-gate-1")
                .documentCode("INCOME_CERTIFICATE")
                .mandatory(true)
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-gate-1")).thenReturn(List.of(doc));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication("app-gate-1", "Approved", "admin-1", "ADMIN")
        );

        assertTrue(ex.getMessage().contains("DOCUMENTS_NOT_VERIFIED"),
                "Exception message must specify DOCUMENTS_NOT_VERIFIED gate failure");
        assertTrue(ex.getMessage().contains("INCOME_CERTIFICATE"));
    }

    @Test
    @DisplayName("2. Approval Gate: Rejects approval when mandatory document is NOT uploaded")
    void testApproveApplication_WhenMandatoryDocNotUploaded_ThrowsException() {
        Application app = Application.builder()
                .id("app-gate-2")
                .applicationNumber("SB-GATE-002")
                .userId("citizen-11")
                .schemeCode("SCHEME_A")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findById("app-gate-2")).thenReturn(Optional.of(app));

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-2")
                .applicationId("app-gate-2")
                .documentCode("AADHAAR_CARD")
                .mandatory(true)
                .uploaded(false)
                .detailedStatus(DetailedDocumentStatus.NOT_UPLOADED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-gate-2")).thenReturn(List.of(doc));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication("app-gate-2", "Approved", "admin-1", "ADMIN")
        );

        assertTrue(ex.getMessage().contains("DOCUMENTS_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("3. Approval Gate Passed: Approves application when all mandatory documents are ADMIN_VERIFIED")
    void testApproveApplication_WhenAllMandatoryDocsAreAdminVerified_Succeeds() {
        Application app = Application.builder()
                .id("app-gate-3")
                .applicationNumber("SB-GATE-003")
                .userId("citizen-12")
                .schemeCode("SCHEME_A")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findById("app-gate-3")).thenReturn(Optional.of(app));

        // Mandatory doc is ADMIN_VERIFIED
        ApplicationDocument docMandatory = ApplicationDocument.builder()
                .id("doc-3a")
                .applicationId("app-gate-3")
                .documentCode("AADHAAR_CARD")
                .mandatory(true)
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.ADMIN_VERIFIED)
                .build();

        // Optional doc is only AI_VERIFIED or unverified
        ApplicationDocument docOptional = ApplicationDocument.builder()
                .id("doc-3b")
                .applicationId("app-gate-3")
                .documentCode("EXTRA_SUPPORTING")
                .mandatory(false)
                .uploaded(true)
                .detailedStatus(DetailedDocumentStatus.AI_VERIFIED)
                .build();

        when(applicationDocumentRepository.findAllByApplicationId("app-gate-3")).thenReturn(List.of(docMandatory, docOptional));
        when(applicationReviewRepository.findByApplicationId("app-gate-3")).thenReturn(Optional.empty());

        ApplicationResponse response = reviewService.approveApplication("app-gate-3", "All criteria satisfied", "officer-42", "NODAL_OFFICER");

        assertNotNull(response);
        assertEquals(ApplicationStatus.APPROVED, app.getStatus());
        verify(applicationRepository).save(app);
        verify(applicationEventService).recordEvent(
                eq("app-gate-3"),
                eq("citizen-12"),
                eq(ApplicationEventType.APPROVED),
                eq(ApplicationStatus.UNDER_REVIEW),
                eq(ApplicationStatus.APPROVED),
                anyString(),
                any()
        );
    }
}
