package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.ApplicationDocument;
import com.schemebridge.scheme.document.DocumentVerificationResult;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.DocumentVerificationResponse;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.DocumentVerificationResultRepository;
import com.schemebridge.scheme.service.ApplicationReviewService;
import com.schemebridge.scheme.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document Operations", description = "Endpoints for document upload, AI verification status, and officer approval")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final ApplicationService applicationService;
    private final ApplicationReviewService applicationReviewService;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DocumentVerificationResultRepository documentVerificationResultRepository;
    private final com.schemebridge.scheme.service.DocumentExtractionService documentExtractionService;

    private String getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && StringUtils.hasText(auth.getName()) ? auth.getName() : "anonymous";
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Extract structured document attributes and verify identity against authenticated citizen")
    public ResponseEntity<com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse> extractDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentCode", required = false) String documentCode,
            @RequestParam(value = "documentType", required = false) String documentType
    ) {
        String userId = getActorId();
        com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse response =
                documentExtractionService.extractAndVerify(file, documentCode, documentType, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Legacy compatibility endpoint for document OCR extraction")
    public ResponseEntity<com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse> extractDocumentOcr(
            @RequestParam(value = "document", required = false) MultipartFile document,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "documentCode", required = false) String documentCode
    ) {
        MultipartFile effectiveFile = file != null ? file : document;
        String userId = getActorId();
        com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse response =
                documentExtractionService.extractAndVerify(effectiveFile, documentCode, documentType, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document for a scheme application (PDF, JPG, PNG under 5MB)")
    public ResponseEntity<ApplicationDocumentResponse> uploadDocument(
            @RequestParam("applicationId") String applicationId,
            @RequestParam("documentCode") String documentCode,
            @RequestParam("file") MultipartFile file
    ) {
        String userId = getActorId();
        ApplicationDocumentResponse response = applicationService.uploadDocument(applicationId, documentCode, file, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{documentId}/verification")
    @Operation(summary = "Get AI verification findings and check breakdown for a document")
    public ResponseEntity<DocumentVerificationResponse> getDocumentVerification(@PathVariable String documentId) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        DocumentVerificationResult result = documentVerificationResultRepository
                .findTopByDocumentIdOrderByVersionDesc(documentId)
                .or(() -> documentVerificationResultRepository.findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(doc.getApplicationId(), doc.getDocumentCode()))
                .orElse(null);

        if (result == null) {
            result = DocumentVerificationResult.builder()
                    .applicationId(doc.getApplicationId())
                    .documentId(doc.getId())
                    .documentCode(doc.getDocumentCode())
                    .documentType(doc.getDocumentType() != null ? doc.getDocumentType() : doc.getDocumentName())
                    .aiStatus(doc.getAiVerificationResult() != null ? doc.getAiVerificationResult() : "AI_REVIEW_REQUIRED")
                    .overallScore(doc.getVerificationScore() != null ? doc.getVerificationScore() : 75.0)
                    .officerStatus(doc.getDetailedStatus() != null && doc.getDetailedStatus().isOfficerVerified() ? "ADMIN_VERIFIED" : "PENDING")
                    .warnings(List.of("AI document validation only. Official identity authentication has not been performed."))
                    .build();
        }

        DocumentVerificationResponse response = DocumentVerificationResponse.builder()
                .id(result.getId())
                .applicationId(result.getApplicationId())
                .documentId(result.getDocumentId())
                .documentCode(result.getDocumentCode())
                .documentType(result.getDocumentType())
                .version(result.getVersion())
                .sha256(result.getSha256())
                .overallScore(result.getOverallScore())
                .aiStatus(result.getAiStatus())
                .officerStatus(result.getOfficerStatus())
                .checks(result.getChecks())
                .extractedFields(result.getExtractedFields())
                .warnings(result.getWarnings())
                .rejectionReasons(result.getRejectionReasons())
                .processedAt(result.getProcessedAt())
                .processingDurationMs(result.getProcessingDurationMs())
                .reviewedBy(result.getReviewedBy())
                .reviewedAt(result.getReviewedAt())
                .remarks(result.getRemarks())
                .duplicateDetected(result.isDuplicateDetected())
                .duplicateReferenceId(result.getDuplicateReferenceId())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{documentId}/verify")
    @Operation(summary = "Officer verifies citizen document")
    public ResponseEntity<ApplicationDocumentResponse> verifyDocument(@PathVariable String documentId) {
        String actorId = getActorId();
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        applicationReviewService.verifyDocument(doc.getApplicationId(), doc.getDocumentCode(), actorId);
        ApplicationDocument updated = applicationDocumentRepository.findById(documentId).orElse(doc);
        return ResponseEntity.ok(applicationService.mapToDocResponse(updated));
    }
}
