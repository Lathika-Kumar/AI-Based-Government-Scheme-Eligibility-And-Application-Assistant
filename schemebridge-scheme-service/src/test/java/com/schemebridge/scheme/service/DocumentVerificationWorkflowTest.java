package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.repository.*;
import com.schemebridge.scheme.service.verification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentVerificationWorkflowTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private ApplicationReviewRepository applicationReviewRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private DocumentVerificationResultRepository resultRepository;

    @Mock
    private ApplicationEventService applicationEventService;

    @Mock
    private ApplicationStatusTransitionService applicationStatusTransitionService;

    @Mock
    private DetailedDocumentStatusTransitionService transitionService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private MongoOperations mongoOperations;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdminAuditService adminAuditService;

    @Mock
    private DocumentOcrProvider ocrProvider;

    private DocumentVerificationService verificationService;
    private ApplicationReviewService reviewService;

    private byte[] createMockPdf(String text) {
        String base = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n" + text + "\n";
        StringBuilder sb = new StringBuilder(base);
        while (sb.length() < 200) {
            sb.append("% padding for test minimum size requirement...\n");
        }
        sb.append("%%EOF\n");
        return sb.toString().getBytes();
    }

    @BeforeEach
    void setUp() {
        SecureFileValidator fileValidator = new SecureFileValidator();
        DocumentQualityAnalyzer qualityAnalyzer = new DocumentQualityAnalyzer();
        ProfileConsistencyChecker consistencyChecker = new ProfileConsistencyChecker();

        List<DocumentValidator> validators = List.of(
                new AadhaarDocumentValidator(),
                new PanDocumentValidator(),
                new IncomeCertificateValidator(),
                new CasteCertificateValidator(),
                new BankPassbookValidator(),
                new DefaultDocumentValidator()
        );

        verificationService = new DocumentVerificationService(
                fileValidator,
                qualityAnalyzer,
                validators,
                consistencyChecker,
                ocrProvider,
                resultRepository,
                applicationDocumentRepository,
                transitionService,
                applicationEventService
        );

        reviewService = new ApplicationReviewService(
                applicationRepository,
                applicationDocumentRepository,
                applicationReviewRepository,
                schemeRepository,
                applicationEventService,
                applicationStatusTransitionService,
                transitionService,
                applicationService,
                mongoOperations,
                notificationService,
                adminAuditService
        );

        lenient().when(ocrProvider.isAvailable()).thenReturn(true);
        lenient().when(resultRepository.save(any(DocumentVerificationResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transitionService.transitionDocumentStatus(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    ApplicationDocument doc = inv.getArgument(0);
                    DetailedDocumentStatus st = inv.getArgument(1);
                    doc.setDetailedStatus(st);
                    if (st == DetailedDocumentStatus.ADMIN_VERIFIED || st == DetailedDocumentStatus.VERIFIED) {
                        doc.setVerificationStatus(DocumentVerificationStatus.VERIFIED);
                    } else if (st == DetailedDocumentStatus.REJECTED || st == DetailedDocumentStatus.CORRECTION_REQUIRED) {
                        doc.setVerificationStatus(DocumentVerificationStatus.REJECTED);
                    }
                    return doc;
                });
        lenient().when(applicationStatusTransitionService.isValidTransition(any(), any())).thenReturn(true);
        lenient().when(applicationRepository.save(any(Application.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(applicationDocumentRepository.save(any(ApplicationDocument.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(applicationService.mapToResponse(any(Application.class))).thenReturn(new ApplicationResponse());
    }

    @Test
    @DisplayName("1. Complete Lifecycle: Upload -> AI Verified -> Approval Blocked -> Officer Verifies -> Approved")
    void testCompleteDocumentVerificationAndApprovalGateWorkflow() {
        String appId = "app-flow-101";
        Application app = Application.builder()
                .id(appId)
                .applicationNumber("SB-FLOW-001")
                .userId("citizen-flow-1")
                .schemeCode("SCH-EDU-001")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-flow-1")
                .applicationId(appId)
                .userId("citizen-flow-1")
                .documentCode("AADHAAR")
                .documentName("Aadhaar Card")
                .mandatory(true)
                .uploaded(true)
                .version(1)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();

        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-flow-1")
                .displayName("Ramesh Sharma")
                .dob(LocalDate.of(1988, 7, 20))
                .state("Rajasthan")
                .build();

        // Step A: AI Verification
        DocumentOcrResult ocrResult = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.96)
                .rawText("GOVERNMENT OF INDIA UNIQUE IDENTIFICATION AUTHORITY OF INDIA\n" +
                        "Ramesh Sharma\nDOB: 20/07/1988 Male\n1234 5678 9999\nRajasthan")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(ocrResult);

        byte[] pdf = createMockPdf("Aadhaar test stream");
        MockMultipartFile file = new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", pdf);

        DocumentVerificationResult aiResult = verificationService.verifyUploadedDocument(doc, file, profile, "citizen-flow-1");

        assertNotNull(aiResult);
        assertEquals("AI_VERIFIED", aiResult.getAiStatus());
        assertTrue(aiResult.getOverallScore() >= 75.0);

        // Aadhaar Privacy Check
        assertNull(aiResult.getExtractedFields().get("aadhaarNumber"), "Plaintext 12-digit Aadhaar number must never be stored");
        assertEquals("9999", aiResult.getExtractedFields().get("aadhaarLast4"));
        assertEquals("XXXX-XXXX-9999", aiResult.getExtractedFields().get("maskedIdentifier"));

        // UIDAI Statutory Disclaimer must be present
        assertTrue(aiResult.getWarnings().stream().anyMatch(w -> w.contains("UIDAI")),
                "Must include mandatory UIDAI disclaimer");

        // Document status is now UNDER_OFFICER_REVIEW, NOT officer-verified
        assertEquals(DetailedDocumentStatus.UNDER_OFFICER_REVIEW, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.PENDING, doc.getVerificationStatus());

        // Step B: Approval Gate must BLOCK approval when document is in UNDER_OFFICER_REVIEW
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationDocumentRepository.findAllByApplicationId(appId)).thenReturn(List.of(doc));

        IllegalStateException gateException = assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication(appId, "Officer approved", "officer-1", "NODAL_OFFICER")
        );
        assertTrue(gateException.getMessage().contains("DOCUMENTS_NOT_VERIFIED"),
                "Gate must reject approval with DOCUMENTS_NOT_VERIFIED");

        // Step C: Officer officially reviews and verifies document
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode(appId, "AADHAAR")).thenReturn(Optional.of(doc));
        reviewService.verifyDocument(appId, "AADHAAR", "officer-1");

        assertEquals(DetailedDocumentStatus.ADMIN_VERIFIED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.VERIFIED, doc.getVerificationStatus());
        assertNotNull(doc.getVerifiedAt());

        // Step D: Now approval gate SUCCEEDS
        ApplicationResponse approvedResponse = reviewService.approveApplication(appId, "All criteria satisfied", "officer-1", "NODAL_OFFICER");
        assertNotNull(approvedResponse);
        assertEquals(ApplicationStatus.APPROVED, app.getStatus());
    }

    @Test
    @DisplayName("2. Correction Workflow: Officer Requests Correction -> Citizen Re-uploads -> Officer Verifies")
    void testCorrectionAndReUploadWorkflow() {
        String appId = "app-flow-102";
        Application app = Application.builder()
                .id(appId)
                .applicationNumber("SB-FLOW-002")
                .userId("citizen-flow-2")
                .schemeCode("SCH-AGRI-001")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-flow-2")
                .applicationId(appId)
                .userId("citizen-flow-2")
                .documentCode("INCOME_CERTIFICATE")
                .documentName("Income Certificate")
                .mandatory(true)
                .uploaded(true)
                .version(1)
                .detailedStatus(DetailedDocumentStatus.AI_REVIEW_REQUIRED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode(appId, "INCOME_CERTIFICATE")).thenReturn(Optional.of(doc));
        when(applicationDocumentRepository.findAllByApplicationId(appId)).thenReturn(List.of(doc));

        // Step 1: Officer requests correction with mandatory reason
        reviewService.requestDocumentCorrection(appId, "INCOME_CERTIFICATE", "Document is blurry, low resolution, or illegible", "officer-2");

        assertEquals(DetailedDocumentStatus.CORRECTION_REQUIRED, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.REJECTED, doc.getVerificationStatus());
        assertEquals("Document is blurry, low resolution, or illegible", doc.getCorrectionReason());

        // Approval must be blocked while correction is pending
        assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication(appId, "premature", "officer-2", "ADMIN")
        );

        // Step 2: Citizen uploads clean replacement (Version 2)
        doc.setVersion(2);
        doc.setUploaded(true);
        doc.setDetailedStatus(DetailedDocumentStatus.UPLOADED);

        CitizenProfile profile2 = CitizenProfile.builder()
                .userId("citizen-flow-2")
                .displayName("Sunita Devi")
                .state("Rajasthan")
                .build();

        DocumentOcrResult cleanOcr = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.95)
                .rawText("GOVERNMENT OF RAJASTHAN REVENUE DEPARTMENT\nINCOME CERTIFICATE\nCertificate No: INC-2026-9999\nAnnual Income: 150000\nName: Sunita Devi\nIssue Date: 12/03/2026\nTahsildar")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(cleanOcr);

        byte[] cleanPdf = createMockPdf("Income clear stream");
        MockMultipartFile cleanFile = new MockMultipartFile("file", "income_v2.pdf", "application/pdf", cleanPdf);

        DocumentVerificationResult v2Result = verificationService.verifyUploadedDocument(doc, cleanFile, profile2, "citizen-flow-2");
        assertNotNull(v2Result);
        assertEquals("AI_VERIFIED", v2Result.getAiStatus());

        // Step 3: Officer verifies the clear document
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        reviewService.verifyDocument(appId, "INCOME_CERTIFICATE", "officer-2");
        assertEquals(DetailedDocumentStatus.ADMIN_VERIFIED, doc.getDetailedStatus());

        // Step 4: Application can now be approved
        ApplicationResponse finalApp = reviewService.approveApplication(appId, "All verified", "officer-2", "ADMIN");
        assertNotNull(finalApp);
        assertEquals(ApplicationStatus.APPROVED, app.getStatus());
    }

    @Test
    @DisplayName("3. Rejection Workflow: Missing rejection reason throws IllegalArgumentException")
    void testRejectionWithoutReason_ThrowsException() {
        String appId = "app-flow-103";

        assertThrows(IllegalArgumentException.class, () ->
                reviewService.rejectDocument(appId, "PAN_CARD", "   ", "officer-3")
        );
    }

    @Test
    @DisplayName("4. AI Flagged Document: Enters UNDER_OFFICER_REVIEW and blocks approval gate")
    void testAiFlaggedEntersUnderOfficerReview_BlocksApproval() {
        String appId = "app-flow-104";
        Application app = Application.builder()
                .id(appId)
                .applicationNumber("SB-FLOW-004")
                .userId("citizen-flow-4")
                .schemeCode("SCH-FLAG-001")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-flow-4")
                .applicationId(appId)
                .userId("citizen-flow-4")
                .documentCode("INCOME_CERTIFICATE")
                .documentName("Income Certificate")
                .mandatory(true)
                .uploaded(true)
                .version(1)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();

        // Simulate blurry OCR result -> triggers AI_REVIEW_REQUIRED / AI_FLAGGED
        DocumentOcrResult blurryOcr = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.PARTIAL)
                .overallConfidence(0.40)
                .rawText("GOVERNMENT REVENUE DEPARTMENT\nINCOME CERTIFICATE\nAnnual Income: Rs 60,000\nBlurry low resolution")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(blurryOcr);

        byte[] pdf = createMockPdf("Blurry income stream");
        MockMultipartFile file = new MockMultipartFile("file", "blurry_income.pdf", "application/pdf", pdf);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, null, "citizen-flow-4");
        assertNotNull(result);
        assertEquals("AI_REVIEW_REQUIRED", result.getAiStatus());

        // Status MUST be UNDER_OFFICER_REVIEW, NOT officer-verified
        assertEquals(DetailedDocumentStatus.UNDER_OFFICER_REVIEW, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.PENDING, doc.getVerificationStatus());
        assertFalse(doc.getDetailedStatus().isOfficerVerified());

        // Approval gate must BLOCK
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationDocumentRepository.findAllByApplicationId(appId)).thenReturn(List.of(doc));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication(appId, "Officer approve", "officer-4", "ADMIN")
        );
        assertTrue(ex.getMessage().contains("DOCUMENTS_NOT_VERIFIED"));
    }

    @Test
    @DisplayName("5. AI Failed Document: Enters UNDER_OFFICER_REVIEW and blocks approval gate")
    void testAiFailedEntersUnderOfficerReview_BlocksApproval() {
        String appId = "app-flow-105";
        Application app = Application.builder()
                .id(appId)
                .applicationNumber("SB-FLOW-005")
                .userId("citizen-flow-5")
                .schemeCode("SCH-FAIL-001")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-flow-5")
                .applicationId(appId)
                .userId("citizen-flow-5")
                .documentCode("PAN_CARD")
                .documentName("PAN Card")
                .mandatory(true)
                .uploaded(true)
                .version(1)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();

        // Mismatched document type -> triggers AI_REJECTED / AI_FAILED
        DocumentOcrResult mismatchOcr = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.95)
                .rawText("Electricity Bill State Power Distribution Ltd")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(mismatchOcr);

        byte[] pdf = createMockPdf("Wrong document content");
        MockMultipartFile file = new MockMultipartFile("file", "wrong_doc.pdf", "application/pdf", pdf);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, null, "citizen-flow-5");
        assertNotNull(result);
        assertEquals("AI_REJECTED", result.getAiStatus());

        // Status MUST be UNDER_OFFICER_REVIEW, NOT officer-verified
        assertEquals(DetailedDocumentStatus.UNDER_OFFICER_REVIEW, doc.getDetailedStatus());
        assertEquals(DocumentVerificationStatus.PENDING, doc.getVerificationStatus());
        assertFalse(doc.getDetailedStatus().isOfficerVerified());

        // Approval gate must BLOCK
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(applicationDocumentRepository.findAllByApplicationId(appId)).thenReturn(List.of(doc));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.approveApplication(appId, "Officer approve", "officer-5", "ADMIN")
        );
        assertTrue(ex.getMessage().contains("DOCUMENTS_NOT_VERIFIED"));
    }
}
