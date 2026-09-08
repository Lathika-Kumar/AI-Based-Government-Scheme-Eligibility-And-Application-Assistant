package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.dto.response.DocumentOcrResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.DocumentOcrResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;

/**
 * Service orchestrating Document OCR extraction, GridFS stream processing,
 * version caching, and unverified provenance tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentOcrService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DocumentOcrResultRepository ocrResultRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentOcrProvider ocrProvider;
    private final DetailedDocumentStatusTransitionService detailedDocumentStatusTransitionService;

    @Transactional
    public DocumentOcrResponse processOcr(String applicationId, String documentCode, String userId, boolean isPrivileged) {
        log.info("Requesting OCR process for application={}, documentCode={}, user={}", applicationId, documentCode, userId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!isPrivileged && !app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to access documents for this application.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with code: " + documentCode));

        if (!doc.isUploaded() || doc.getStorageReference() == null) {
            throw new IllegalStateException("Document has not been uploaded yet.");
        }

        int version = doc.getVersion() != null ? doc.getVersion() : 1;

        // Transition to OCR_PROCESSING
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.OCR_PROCESSING, userId, "OCR processing initiated.");

        // Check if OCR result already cached for this exact version
        Optional<DocumentOcrResult> existing = ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion(
                applicationId, documentCode, version);

        if (existing.isPresent()) {
            log.info("Returning cached OCR result for application={}, docCode={}, version={}", applicationId, documentCode, version);
            detailedDocumentStatusTransitionService.transitionDocumentStatus(
                    doc, DetailedDocumentStatus.OCR_COMPLETED, userId, "Cached OCR result retrieved.");
            return mapToResponse(existing.get());
        }

        // Process through OCR provider
        InputStream stream = documentStorageService.retrieve(doc.getStorageReference());
        DocumentOcrResult result = ocrProvider.process(stream, documentCode, doc.getFileName(), doc.getContentType());

        result.setApplicationId(applicationId);
        result.setDocumentId(doc.getId());
        result.setDocumentCode(documentCode);
        result.setUserId(app.getUserId());
        result.setVersion(version);

        DocumentOcrResult saved = ocrResultRepository.save(result);
        log.info("Saved new OCR result id={}, status={}", saved.getId(), saved.getExtractionStatus());

        // Transition to OCR_COMPLETED
        detailedDocumentStatusTransitionService.transitionDocumentStatus(
                doc, DetailedDocumentStatus.OCR_COMPLETED, userId, "OCR processing successfully completed.");

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentOcrResponse getOcrResult(String applicationId, String documentCode, String userId, boolean isPrivileged) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + applicationId));

        if (!isPrivileged && !app.getUserId().equals(userId)) {
            throw new SecurityException("You do not have permission to access documents for this application.");
        }

        ApplicationDocument doc = applicationDocumentRepository.findByApplicationIdAndDocumentCode(applicationId, documentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with code: " + documentCode));

        int version = doc.getVersion() != null ? doc.getVersion() : 1;

        DocumentOcrResult result = ocrResultRepository.findByApplicationIdAndDocumentCodeAndVersion(
                applicationId, documentCode, version)
                .orElseThrow(() -> new ResourceNotFoundException("No OCR result found for document version " + version));

        return mapToResponse(result);
    }

    public DocumentOcrResponse mapToResponse(DocumentOcrResult r) {
        return DocumentOcrResponse.builder()
                .id(r.getId())
                .applicationId(r.getApplicationId())
                .documentId(r.getDocumentId())
                .documentCode(r.getDocumentCode())
                .userId(r.getUserId())
                .version(r.getVersion())
                .detectedDocumentType(r.getDetectedDocumentType())
                .rawText(r.getRawText())
                .extractedFields(r.getExtractedFields())
                .overallConfidence(r.getOverallConfidence())
                .extractionStatus(r.getExtractionStatus())
                .provider(r.getProvider())
                .providerVersion(r.getProviderVersion())
                .processedAt(r.getProcessedAt())
                .processingDurationMs(r.getProcessingDurationMs())
                .build();
    }
}
