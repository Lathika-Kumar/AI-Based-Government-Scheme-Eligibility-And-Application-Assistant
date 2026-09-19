package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.DocumentVerificationResultRepository;
import com.schemebridge.scheme.service.verification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentVerificationServiceTest {

    @Mock
    private DocumentOcrProvider ocrProvider;

    @Mock
    private DocumentVerificationResultRepository resultRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private DetailedDocumentStatusTransitionService transitionService;

    @Mock
    private ApplicationEventService applicationEventService;

    private DocumentVerificationService verificationService;

    private byte[] createMockPdfBytes(String text) {
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

        lenient().when(ocrProvider.isAvailable()).thenReturn(true);
        lenient().when(resultRepository.save(any(DocumentVerificationResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transitionService.transitionDocumentStatus(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ApplicationDocument doc = invocation.getArgument(0);
                    DetailedDocumentStatus st = invocation.getArgument(1);
                    doc.setDetailedStatus(st);
                    return doc;
                });
    }

    @Test
    @DisplayName("1. Aadhaar Card: Valid Extraction, UIDAI Disclaimer, and Strict Masking")
    void testAadhaarCard_ValidExtraction_MaskedPrivacy_AndUidaiDisclaimer() {
        byte[] pdfBytes = createMockPdfBytes("Sample Aadhaar Card");
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar_sample.pdf", "application/pdf", pdfBytes
        );

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-aadhaar-1")
                .applicationId("app-100")
                .documentCode("AADHAAR_CARD")
                .documentName("Aadhaar Card")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .build();

        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-1")
                .displayName("Rajesh Kumar")
                .dob(LocalDate.of(1990, 5, 15))
                .state("Maharashtra")
                .build();

        // Mock OCR result
        DocumentOcrResult ocrResult = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.95)
                .rawText("GOVERNMENT OF INDIA UNIQUE IDENTIFICATION AUTHORITY OF INDIA\n" +
                        "Rajesh Kumar\nDOB: 15/05/1990 Male\n1234 5678 9012\nMaharashtra")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(ocrResult);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, profile, "citizen-1");

        assertNotNull(result);
        assertEquals("AI_VERIFIED", result.getAiStatus());
        assertTrue(result.getOverallScore() >= 80.0, "Score should be >= 80 for valid Aadhaar");

        // Privacy check: No plain text 12-digit number
        assertNull(result.getExtractedFields().get("aadhaarNumber"));
        assertEquals("9012", result.getExtractedFields().get("aadhaarLast4"));
        assertEquals("XXXX-XXXX-9012", result.getExtractedFields().get("maskedIdentifier"));

        // UIDAI Statutory Disclaimer must be present
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("UIDAI")),
                "Must include mandatory UIDAI disclaimer");

        // Status moved to AI_PASSED then UNDER_OFFICER_REVIEW, NOT ADMIN_VERIFIED
        verify(transitionService).transitionDocumentStatus(
                eq(doc),
                eq(DetailedDocumentStatus.AI_PASSED),
                nullable(String.class),
                anyString()
        );
        verify(transitionService).transitionDocumentStatus(
                eq(doc),
                eq(DetailedDocumentStatus.UNDER_OFFICER_REVIEW),
                nullable(String.class),
                anyString()
        );
        assertEquals(DetailedDocumentStatus.UNDER_OFFICER_REVIEW, doc.getDetailedStatus());
    }

    @Test
    @DisplayName("2. Mismatched Document Type: Aadhaar uploaded for Income Certificate requirement flags failure/review")
    void testMismatchedDocumentType_FlagsAiReviewRequired() {
        byte[] pdfBytes = createMockPdfBytes("Sample Document");
        MockMultipartFile file = new MockMultipartFile(
                "file", "aadhaar_as_income.pdf", "application/pdf", pdfBytes
        );

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-income-1")
                .applicationId("app-101")
                .documentCode("INCOME_CERTIFICATE")
                .documentName("Income Certificate")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .build();

        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-2")
                .displayName("Sunita Devi")
                .build();

        DocumentOcrResult ocrResult = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.92)
                .rawText("UNIQUE IDENTIFICATION AUTHORITY OF INDIA Aadhaar Card Sunita Devi 5555 6666 7777")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(ocrResult);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, profile, "citizen-2");

        assertNotNull(result);
        VerificationCheckDetail typeCheck = result.getChecks().stream()
                .filter(c -> "DOCUMENT_TYPE".equals(c.getCheck()))
                .findFirst()
                .orElse(null);

        assertNotNull(typeCheck);
        assertEquals("FAILED", typeCheck.getStatus());
        assertEquals("AI_REJECTED", result.getAiStatus());
        assertTrue(result.getRejectionReasons().stream().anyMatch(w -> w.contains("Income Certificate")),
                "Rejection reasons should flag missing Income Certificate markers");
    }

    @Test
    @DisplayName("3. Blurry or Corrupted File: Triggers AI Review or Quality Penalty")
    void testBlurryOrUnreadableDocument_HandlesFailureGracefully() {
        byte[] pdfBytes = createMockPdfBytes("Empty Text Scan");
        MockMultipartFile file = new MockMultipartFile(
                "file", "blurry_scan.pdf", "application/pdf", pdfBytes
        );

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-blurry-1")
                .applicationId("app-102")
                .documentCode("PAN_CARD")
                .documentName("PAN Card")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .build();

        DocumentOcrResult ocrResult = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.LOW_CONFIDENCE)
                .overallConfidence(0.2)
                .rawText("")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(ocrResult);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, null, "citizen-3");

        assertNotNull(result);
        assertTrue(result.getOverallScore() < 50.0);

        VerificationCheckDetail qualityCheck = result.getChecks().stream()
                .filter(c -> "OCR_QUALITY".equals(c.getCheck()))
                .findFirst()
                .orElse(null);

        assertNotNull(qualityCheck);
        assertEquals("FAILED", qualityCheck.getStatus());
    }

    @Test
    @DisplayName("4. Consistency Checker: Missing profile attributes must yield NOT_CHECKED (0 pts) rather than artificial positive points")
    void testConsistencyChecker_MissingProfileAttributes_YieldsNotChecked() {
        byte[] pdfBytes = createMockPdfBytes("Bank Passbook");
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", pdfBytes
        );

        ApplicationDocument doc = ApplicationDocument.builder()
                .id("doc-passbook-1")
                .applicationId("app-103")
                .documentCode("BANK_PASSBOOK")
                .documentName("Bank Passbook")
                .mandatory(true)
                .detailedStatus(DetailedDocumentStatus.UPLOADED)
                .build();

        CitizenProfile profile = CitizenProfile.builder()
                .userId("citizen-4")
                .displayName("Amit Patel")
                .build();

        DocumentOcrResult ocrResult = DocumentOcrResult.builder()
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.90)
                .rawText("State Bank of India Account No: 123456789012 IFSC: SBIN0001234 Amit Patel")
                .build();
        when(ocrProvider.process(any(), any(), any(), any())).thenReturn(ocrResult);

        DocumentVerificationResult result = verificationService.verifyUploadedDocument(doc, file, profile, "citizen-4");

        VerificationCheckDetail dobCheck = result.getChecks().stream()
                .filter(c -> "DOB_MATCH".equals(c.getCheck()))
                .findFirst()
                .orElse(null);

        assertNotNull(dobCheck);
        assertEquals("NOT_CHECKED", dobCheck.getStatus());
        assertEquals(0.0, dobCheck.getScore());
    }
}
