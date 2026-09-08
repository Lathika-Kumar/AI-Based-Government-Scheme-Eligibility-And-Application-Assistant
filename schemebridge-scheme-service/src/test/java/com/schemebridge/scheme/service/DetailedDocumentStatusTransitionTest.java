package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DetailedDocumentStatusTransitionTest {

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private ApplicationEventService applicationEventService;

    private DetailedDocumentStatusTransitionService transitionService;

    @BeforeEach
    void setUp() {
        transitionService = new DetailedDocumentStatusTransitionService(
                applicationDocumentRepository,
                applicationEventService
        );
        lenient().when(applicationDocumentRepository.save(any(ApplicationDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("1. Legal Transition Chain: NOT_UPLOADED -> UPLOADED -> OCR_PROCESSING -> OCR_COMPLETED -> PENDING -> VERIFIED")
    void testLegalTransitionChain_Success() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-1")
                .applicationId("app-1")
                .documentCode("INCOME_PROOF")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.NOT_UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .version(1)
                .build();

        // 1. NOT_UPLOADED -> UPLOADED
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.UPLOADED, "user-65", "Initial upload");
        assertEquals(DetailedDocumentStatus.UPLOADED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.PENDING, doc.getVerificationStatus());
        assertTrue(doc.isUploaded());

        // 2. UPLOADED -> OCR_PROCESSING
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.OCR_PROCESSING, "user-65", "OCR started");
        assertEquals(DetailedDocumentStatus.OCR_PROCESSING, doc.getDetailedStatus());

        // 3. OCR_PROCESSING -> OCR_COMPLETED
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.OCR_COMPLETED, "user-65", "OCR finished");
        assertEquals(DetailedDocumentStatus.OCR_COMPLETED, doc.getDetailedStatus());

        // 4. OCR_COMPLETED -> PENDING_ADMIN_VERIFICATION
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION, "user-65", "Ready for review");
        assertEquals(DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION, doc.getDetailedStatus());

        // 5. PENDING_ADMIN_VERIFICATION -> VERIFIED
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.VERIFIED, "admin-1", "Verified by officer");
        assertEquals(DetailedDocumentStatus.VERIFIED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.VERIFIED, doc.getVerificationStatus());
        assertNotNull(doc.getVerifiedAt());
        assertEquals("admin-1", doc.getVerifiedBy());
    }

    @Test
    @DisplayName("2. Rejection Lifecycle: PENDING -> REJECTED -> REUPLOAD_REQUIRED -> UPLOADED")
    void testRejectionAndReuploadLifecycle() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-2")
                .applicationId("app-2")
                .documentCode("CASTE_CERT")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .version(1)
                .build();

        // Admin rejects document
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.REJECTED, "admin-1", "Image is blurry; illegible seal.");
        assertEquals(DetailedDocumentStatus.REJECTED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.REJECTED, doc.getVerificationStatus());
        assertEquals("Image is blurry; illegible seal.", doc.getRejectionReason());
        assertNotNull(doc.getRejectedAt());

        // Transition to REUPLOAD_REQUIRED
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.REUPLOAD_REQUIRED, "admin-1", "Please re-scan at 300 DPI.");
        assertEquals(DetailedDocumentStatus.REUPLOAD_REQUIRED, doc.getDetailedStatus());

        // Citizen re-uploads (version 2)
        doc.setVersion(2);
        doc = transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.UPLOADED, "user-65", "Uploaded replacement version 2");
        assertEquals(DetailedDocumentStatus.UPLOADED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.PENDING, doc.getVerificationStatus());
        assertNull(doc.getRejectionReason());
    }

    @Test
    @DisplayName("3. Mandatory Rejection Reason Validation")
    void testRejectionWithoutReason_ThrowsException() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-3")
                .applicationId("app-3")
                .documentCode("AADHAAR")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.PENDING_ADMIN_VERIFICATION)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.REJECTED, "admin-1", null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.REJECTED, "admin-1", "   ")
        );
    }

    @Test
    @DisplayName("4. Illegal Direct Transition: NOT_UPLOADED -> VERIFIED throws IllegalStateException")
    void testIllegalTransition_ThrowsException() {
        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-4")
                .applicationId("app-4")
                .documentCode("LAND_RECORD")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.NOT_UPLOADED)
                .build();

        assertThrows(IllegalStateException.class, () ->
                transitionService.transitionDocumentStatus(doc, DetailedDocumentStatus.VERIFIED, "admin-1", "Direct verify")
        );
    }
}
