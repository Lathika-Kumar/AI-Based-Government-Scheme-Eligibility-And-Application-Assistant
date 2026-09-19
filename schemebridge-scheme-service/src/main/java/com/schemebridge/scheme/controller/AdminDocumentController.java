package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.ApplicationDocument;
import com.schemebridge.scheme.document.DocumentVerificationResult;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.dto.request.DocumentReviewDecisionRequest;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.DocumentVerificationResponse;
import com.schemebridge.scheme.dto.response.PagedApplicationDocumentResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Document Repository", description = "Administrative endpoints for cross-application document verification repository")
@SecurityRequirement(name = "BearerAuth")
public class AdminDocumentController {

    private final MongoTemplate mongoTemplate;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final DocumentVerificationResultRepository documentVerificationResultRepository;
    private final ApplicationReviewService applicationReviewService;
    private final ApplicationService applicationService;

    private String getReviewerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && StringUtils.hasText(auth.getName()) ? auth.getName() : "admin";
    }

    @GetMapping
    @Operation(summary = "Get paginated cross-application document repository queue (Admin only)")
    public ResponseEntity<PagedApplicationDocumentResponse> getDocumentsQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schemeCode,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadedAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Only include uploaded documents
        criteriaList.add(Criteria.where("uploaded").is(true));

        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                criteriaList.add(Criteria.where("verificationStatus").is(DocumentVerificationStatus.valueOf(status.trim().toUpperCase())));
            } catch (IllegalArgumentException ignored) {}
        }

        if (schemeCode != null && !schemeCode.isBlank() && !"all".equalsIgnoreCase(schemeCode)) {
            criteriaList.add(Criteria.where("schemeCode").is(schemeCode.trim()));
        }

        if (search != null && !search.isBlank()) {
            String term = search.trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("fileName").regex(term, "i"),
                    Criteria.where("documentCode").regex(term, "i"),
                    Criteria.where("documentName").regex(term, "i"),
                    Criteria.where("applicationId").regex(term, "i"),
                    Criteria.where("userId").regex(term, "i")
            ));
        }

        query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));

        long totalElements = mongoTemplate.count(query, ApplicationDocument.class);

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction != null ? direction : "DESC"), sort != null ? sort : "uploadedAt");
        Pageable pageable = PageRequest.of(page, size, sortObj);
        query.with(pageable);

        List<ApplicationDocument> docs = mongoTemplate.find(query, ApplicationDocument.class);
        List<ApplicationDocumentResponse> content = docs.stream()
                .map(applicationService::mapToDocResponse)
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PagedApplicationDocumentResponse response = PagedApplicationDocumentResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get single document metadata by document ID")
    public ResponseEntity<ApplicationDocumentResponse> getDocumentById(@PathVariable String documentId) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));
        return ResponseEntity.ok(applicationService.mapToDocResponse(doc));
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download/stream uploaded document binary by document ID (Admin only)")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadDocumentById(@PathVariable String documentId) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        var downloadDto = applicationService.getDocumentDownload(doc.getApplicationId(), doc.getDocumentCode(), null, true);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadDto.getFileName() + "\"");
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(downloadDto.getContentType()));
        if (downloadDto.getFileSize() != null && downloadDto.getFileSize() > 0) {
            headers.setContentLength(downloadDto.getFileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new org.springframework.core.io.InputStreamResource(downloadDto.getInputStream()));
    }

    @GetMapping("/{documentId}/verification")
    @Operation(summary = "Get AI verification analysis and check findings for a document")
    public ResponseEntity<DocumentVerificationResponse> getDocumentVerification(@PathVariable String documentId) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        DocumentVerificationResult result = documentVerificationResultRepository
                .findTopByDocumentIdOrderByVersionDesc(documentId)
                .or(() -> documentVerificationResultRepository.findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(doc.getApplicationId(), doc.getDocumentCode()))
                .orElse(null);

        if (result == null) {
            // Build fallback representation
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
    @Operation(summary = "Officer approval of citizen document")
    public ResponseEntity<ApplicationDocumentResponse> verifyDocument(
            @PathVariable String documentId,
            @RequestBody(required = false) DocumentReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        applicationReviewService.verifyDocument(doc.getApplicationId(), doc.getDocumentCode(), reviewerId);
        ApplicationDocument updated = applicationDocumentRepository.findById(documentId).orElse(doc);
        return ResponseEntity.ok(applicationService.mapToDocResponse(updated));
    }

    @GetMapping("/pending-verification")
    @Operation(summary = "Get documents pending officer verification")
    public ResponseEntity<PagedApplicationDocumentResponse> getPendingVerificationDocuments(
            @RequestParam(required = false) String schemeCode,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadedAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        return getDocumentsQueue("PENDING", schemeCode, search, page, size, sort, direction);
    }

    @PostMapping({ "/{documentId}/correction", "/{documentId}/request-correction" })
    @Operation(summary = "Officer requests correction/re-upload of citizen document")
    public ResponseEntity<ApplicationDocumentResponse> requestCorrection(
            @PathVariable String documentId,
            @RequestBody(required = false) DocumentReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        String reason = request != null && StringUtils.hasText(request.getReason())
                ? request.getReason()
                : (request != null && StringUtils.hasText(request.getCorrectionReason())
                ? request.getCorrectionReason()
                : "Document requires correction or re-upload.");

        applicationReviewService.requestDocumentCorrection(doc.getApplicationId(), doc.getDocumentCode(), reason, reviewerId);
        ApplicationDocument updated = applicationDocumentRepository.findById(documentId).orElse(doc);
        return ResponseEntity.ok(applicationService.mapToDocResponse(updated));
    }

    @PostMapping("/{documentId}/reject")
    @Operation(summary = "Officer rejects citizen document with mandatory reason")
    public ResponseEntity<ApplicationDocumentResponse> rejectDocument(
            @PathVariable String documentId,
            @RequestBody(required = false) DocumentReviewDecisionRequest request
    ) {
        String reviewerId = getReviewerId();
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + documentId));

        String reason = request != null && StringUtils.hasText(request.getReason())
                ? request.getReason()
                : (request != null && StringUtils.hasText(request.getRemarks())
                ? request.getRemarks()
                : null);

        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Rejection reason is mandatory.");
        }

        applicationReviewService.rejectDocument(doc.getApplicationId(), doc.getDocumentCode(), reason, reviewerId);
        ApplicationDocument updated = applicationDocumentRepository.findById(documentId).orElse(doc);
        return ResponseEntity.ok(applicationService.mapToDocResponse(updated));
    }
}
