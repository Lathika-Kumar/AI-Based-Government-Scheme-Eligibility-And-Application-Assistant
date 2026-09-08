package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.DocumentOcrResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.ocr.NativePdfAndRegexDocumentOcrProvider;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.DocumentOcrResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentOcrServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Mock
    private DocumentOcrResultRepository ocrResultRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;

    @Spy
    private DocumentOcrProvider ocrProvider = new NativePdfAndRegexDocumentOcrProvider();

    @InjectMocks
    private DocumentOcrService documentOcrService;

    private Application mockApp;
    private ApplicationDocument mockDoc;

    @BeforeEach
    void setUp() {
        lenient().when(detailedDocumentStatusTransitionService.transitionDocumentStatus(any(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));

        mockApp = Application.builder()
                .id("app-123")
                .userId("65")
                .schemeCode("PM_KISAN_2026")
                .status(ApplicationStatus.DOCUMENTS_PENDING)
                .build();

        mockDoc = ApplicationDocument.builder()
                .id("doc-001")
                .applicationId("app-123")
                .documentCode("INCOME_CERT")
                .fileName("income_certificate.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .storageReference("gridfs-ref-123")
                .uploaded(true)
                .version(1)
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Process OCR: Successfully extracts annualIncome and certificateNumber from income certificate text stream")
    void testProcessOcr_IncomeCertificate_Success() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(mockApp));
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode("app-123", "INCOME_CERT"))
                .thenReturn(Optional.of(mockDoc));
        when(ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion("app-123", "INCOME_CERT", 1))
                .thenReturn(Optional.empty());

        String sampleText = "GOVERNMENT OF MAHARASHTRA\nREVENUE DEPARTMENT\nCertificate No: MH/INC/2026/098412\n" +
                "This is to certify that Rajesh Patel has Annual Income: Rs. 160000 per annum.";
        when(documentStorageService.retrieve("gridfs-ref-123"))
                .thenReturn(new ByteArrayInputStream(sampleText.getBytes(StandardCharsets.UTF_8)));

        when(ocrResultRepository.save(any(DocumentOcrResult.class))).thenAnswer(i -> {
            DocumentOcrResult r = i.getArgument(0);
            r.setId("ocr-result-001");
            return r;
        });

        DocumentOcrResponse response = documentOcrService.processOcr("app-123", "INCOME_CERT", "65", false);

        assertNotNull(response);
        assertEquals("app-123", response.getApplicationId());
        assertEquals("INCOME_CERT", response.getDocumentCode());
        assertEquals("65", response.getUserId());
        assertEquals(1, response.getVersion());
        assertEquals(OcrExtractionStatus.SUCCESS, response.getExtractionStatus());
        assertTrue(response.getOverallConfidence() >= 0.85);

        Map<String, ExtractedFieldDetail> fields = response.getExtractedFields();
        assertNotNull(fields);
        assertTrue(fields.containsKey("annualIncome"));
        assertEquals(160000.0, fields.get("annualIncome").getValue());
        assertEquals("OCR_EXTRACTED", fields.get("annualIncome").getSource());
        assertFalse(fields.get("annualIncome").isVerified());

        verify(ocrResultRepository, times(1)).save(any(DocumentOcrResult.class));
    }

    @Test
    @DisplayName("Process OCR: Returns cached result when document version was already processed")
    void testProcessOcr_ReturnsCached() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(mockApp));
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode("app-123", "INCOME_CERT"))
                .thenReturn(Optional.of(mockDoc));

        DocumentOcrResult cached = DocumentOcrResult.builder()
                .id("ocr-cached-001")
                .applicationId("app-123")
                .documentCode("INCOME_CERT")
                .version(1)
                .extractionStatus(OcrExtractionStatus.SUCCESS)
                .overallConfidence(0.95)
                .processedAt(Instant.now())
                .build();

        when(ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion("app-123", "INCOME_CERT", 1))
                .thenReturn(Optional.of(cached));

        DocumentOcrResponse response = documentOcrService.processOcr("app-123", "INCOME_CERT", "65", false);

        assertNotNull(response);
        assertEquals("ocr-cached-001", response.getId());
        verify(documentStorageService, never()).retrieve(any());
    }

    @Test
    @DisplayName("Process OCR: IDOR Security Exception thrown when user does not own application")
    void testProcessOcr_UnauthorizedUser_ThrowsSecurityException() {
        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(mockApp));

        assertThrows(SecurityException.class, () ->
                documentOcrService.processOcr("app-123", "INCOME_CERT", "different_user_99", false));
    }

    @Test
    @DisplayName("Process OCR: Throws IllegalStateException when document has not been uploaded yet")
    void testProcessOcr_UnuploadedDocument_ThrowsException() {
        mockDoc.setUploaded(false);
        mockDoc.setStorageReference(null);

        when(applicationRepository.findById("app-123")).thenReturn(Optional.of(mockApp));
        when(applicationDocumentRepository.findByApplicationIdAndDocumentCode("app-123", "INCOME_CERT"))
                .thenReturn(Optional.of(mockDoc));

        assertThrows(IllegalStateException.class, () ->
                documentOcrService.processOcr("app-123", "INCOME_CERT", "65", false));
    }
}
