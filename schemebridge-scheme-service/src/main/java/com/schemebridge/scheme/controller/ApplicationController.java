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

    @GetMapping("/{applicationId}/documents")
    @Operation(summary = "Get list of required and uploaded documents for an application")
    public ResponseEntity<List<ApplicationDocumentResponse>> getApplicationDocuments(@PathVariable String applicationId) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SCHEME_MANAGER") || a.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
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
        boolean isPrivileged = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SCHEME_MANAGER") || a.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
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
        boolean isPrivileged = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SCHEME_MANAGER") || a.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
        
        var downloadDto = applicationService.getDocumentDownload(applicationId, documentCode, userId, isPrivileged);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadDto.getFileName() + "\"");
        headers.setContentType(MediaType.parseMediaType(downloadDto.getContentType()));
        if (downloadDto.getFileSize() != null && downloadDto.getFileSize() > 0) {
            headers.setContentLength(downloadDto.getFileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new org.springframework.core.io.InputStreamResource(downloadDto.getInputStream()));
    }

    @GetMapping("/{applicationId}/timeline")
    @Operation(summary = "Get chronological history/timeline of events for a scheme application")
    public ResponseEntity<ApplicationTimelineResponse> getTimeline(@PathVariable String applicationId) {
        String userId = getUserId();
        ApplicationTimelineResponse response = applicationService.getTimeline(applicationId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

