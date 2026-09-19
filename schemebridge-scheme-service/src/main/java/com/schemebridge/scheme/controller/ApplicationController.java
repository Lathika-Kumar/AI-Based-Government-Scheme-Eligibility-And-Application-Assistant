package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.ApplicationTimelineResponse;
import com.schemebridge.scheme.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Application Management", description = "Endpoints for managing citizen scheme applications")
@SecurityRequirement(name = "BearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final com.schemebridge.scheme.repository.DocumentVerificationResultRepository documentVerificationResultRepository;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Unauthorized request");
        }
        return auth.getName();
    }

    @PostMapping
    @Operation(summary = "Create a new scheme application for eligible citizens")
    public ResponseEntity<ApplicationResponse> createApplication(@Valid @RequestBody CreateApplicationRequest request) {
        String userId = getUserId();
        ApplicationResponse response = applicationService.createApplication(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping({"/{applicationId}", "/{applicationId}/status"})
    @Operation(summary = "Get detailed status, readiness percentage, and document list for an application")
    public ResponseEntity<ApplicationResponse> getApplicationDetails(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationResponse response = applicationService.getApplicationDetails(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/my")
    @Operation(summary = "Get all applications belonging to the authenticated citizen")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        String userId = getUserId();
        List<ApplicationResponse> response = applicationService.getMyApplications(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/{applicationId}/documents/{documentCode}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a required document for the application (files must be under 5MB, PDF/JPG/PNG/JPEG formats only)")
    public ResponseEntity<ApplicationDocumentResponse> uploadDocument(
            @PathVariable String applicationId,
            @PathVariable String documentCode,
            @RequestParam("file") MultipartFile file) {
        String userId = getUserId();
        ApplicationDocumentResponse response = applicationService.uploadDocument(applicationId, documentCode, file, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/submit")
    @Operation(summary = "Submit the application when all mandatory documents are uploaded")
    public ResponseEntity<ApplicationResponse> submitApplication(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationResponse response = applicationService.submitApplication(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/cancel")
    @Operation(summary = "Cancel a draft or submitted application before it reaches a terminal status")
    public ResponseEntity<ApplicationResponse> cancelApplication(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationResponse response = applicationService.cancelApplication(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{applicationId}/reapply")
    @Operation(summary = "Reapply for a previously REJECTED or CANCELLED application, creating a new application while reusing citizen vault documents")
    public ResponseEntity<ApplicationResponse> reapplyApplication(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationResponse response = applicationService.reapplyApplication(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private boolean isPrivilegedUser(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a -> {
            String authority = a.getAuthority().toUpperCase();
            String normalized = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
            return normalized.equals("ADMIN") ||
                   normalized.equals("SUPER_ADMIN") ||
                   normalized.equals("SCHEME_MANAGER") ||
                   normalized.equals("VERIFICATION_OFFICER") ||
                   normalized.equals("OFFICER");
        });
    }

    @GetMapping("/{applicationId}/documents")
    @Operation(summary = "Get list of required and uploaded documents for an application")
    public ResponseEntity<List<ApplicationDocumentResponse>> getApplicationDocuments(@PathVariable String applicationId) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = isPrivilegedUser(auth);
        List<ApplicationDocumentResponse> response = applicationService.getApplicationDocuments(applicationId, userId, isPrivileged);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{applicationId}/documents/{documentCode}")
    @Operation(summary = "Get status and details of a single application document")
    public ResponseEntity<ApplicationDocumentResponse> getApplicationDocument(
            @PathVariable String applicationId,
            @PathVariable String documentCode) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = isPrivilegedUser(auth);
        ApplicationDocumentResponse response = applicationService.getApplicationDocument(applicationId, documentCode, userId, isPrivileged);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{applicationId}/documents/{documentCode}/download")
    @Operation(summary = "Download/stream the uploaded binary document from MongoDB GridFS")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadDocument(
            @PathVariable String applicationId,
            @PathVariable String documentCode) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = isPrivilegedUser(auth);
        
        var downloadDto = applicationService.getDocumentDownload(applicationId, documentCode, userId, isPrivileged);
        
        String contentType = downloadDto.getContentType();
        if (contentType == null || contentType.isBlank() || contentType.equals("application/octet-stream")) {
            String fName = downloadDto.getFileName() != null ? downloadDto.getFileName().toLowerCase() : "";
            if (fName.endsWith(".pdf")) contentType = "application/pdf";
            else if (fName.endsWith(".jpg") || fName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fName.endsWith(".png")) contentType = "image/png";
            else contentType = "application/octet-stream";
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadDto.getFileName() + "\"");
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (downloadDto.getFileSize() != null && downloadDto.getFileSize() > 0) {
            headers.setContentLength(downloadDto.getFileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new org.springframework.core.io.InputStreamResource(downloadDto.getInputStream()));
    }

    @GetMapping("/{applicationId}/document-readiness")
    @Operation(summary = "Get application document readiness details")
    public ResponseEntity<com.schemebridge.scheme.dto.response.DocumentReadinessResponse> getDocumentReadiness(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationResponse app = applicationService.getApplicationDetails(applicationId, userId);
        return ResponseEntity.ok(app.getDocumentReadiness());
    }

    @GetMapping("/{applicationId}/documents/{documentCode}/verification")
    @Operation(summary = "Get AI verification analysis and findings for an application document")
    public ResponseEntity<com.schemebridge.scheme.dto.response.DocumentVerificationResponse> getDocumentVerification(
            @PathVariable String applicationId,
            @PathVariable String documentCode) {
        String userId = getUserId();
        // Ensure user has access to this application
        applicationService.getApplicationDetails(applicationId, userId);

        return documentVerificationResultRepository
                .findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(applicationId, documentCode)
                .map(res -> ResponseEntity.ok(com.schemebridge.scheme.dto.response.DocumentVerificationResponse.builder()
                        .id(res.getId())
                        .applicationId(res.getApplicationId())
                        .documentId(res.getDocumentId())
                        .documentCode(res.getDocumentCode())
                        .documentType(res.getDocumentType())
                        .version(res.getVersion())
                        .sha256(res.getSha256())
                        .overallScore(res.getOverallScore())
                        .aiStatus(res.getAiStatus())
                        .officerStatus(res.getOfficerStatus())
                        .checks(res.getChecks())
                        .extractedFields(res.getExtractedFields())
                        .warnings(res.getWarnings())
                        .rejectionReasons(res.getRejectionReasons())
                        .processedAt(res.getProcessedAt())
                        .processingDurationMs(res.getProcessingDurationMs())
                        .reviewedBy(res.getReviewedBy())
                        .reviewedAt(res.getReviewedAt())
                        .remarks(res.getRemarks())
                        .duplicateDetected(res.isDuplicateDetected())
                        .duplicateReferenceId(res.getDuplicateReferenceId())
                        .build()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{applicationId}/timeline")
    @Operation(summary = "Get chronological history/timeline of events for a scheme application")
    public ResponseEntity<ApplicationTimelineResponse> getTimeline(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationTimelineResponse response = applicationService.getTimeline(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

