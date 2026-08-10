package com.schemebridge.documentservice.controller;

import com.schemebridge.documentservice.dto.*;
import com.schemebridge.documentservice.enums.DocumentType;
import com.schemebridge.documentservice.service.DocumentService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "Citizen document upload, storage, verification, download, versioning, and readiness APIs")
public class DocumentController {

    private final DocumentService documentService;

    // ─── UPLOAD ──────────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Document",
               description = "Uploads a citizen document (PDF, PNG, JPG). Calculates SHA-256 checksum to prevent duplicate uploads. Preserves version history on re-upload.")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("authUserId") String authUserId,
            @RequestParam(value = "applicationId", required = false) String applicationId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "documentName", required = false) String documentName) {
        DocumentResponse response = documentService.uploadDocument(file, authUserId, applicationId, documentType, documentName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get Document Metadata", description = "Retrieves document metadata, version history, and audit trail by document ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Document metadata retrieved", documentService.getDocumentById(id)));
    }

    // ─── DOWNLOAD / PREVIEW ──────────────────────────────────────────────────────

    @GetMapping("/download/{id}")
    @Operation(summary = "Download / Preview Document", description = "Downloads or previews the physical document file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String id) {
        DocumentResponse metadata = documentService.getDocumentById(id);
        Resource resource = documentService.downloadDocumentResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(resource);
    }

    // ─── APPLICATION DOCUMENTS ───────────────────────────────────────────────────

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Get Application Documents", description = "Returns all uploaded documents associated with a specific scheme application")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByApplicationId(@PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.success("Application documents retrieved", documentService.getDocumentsByApplicationId(applicationId)));
    }

    // ─── MY DOCUMENTS ────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get My Documents", description = "Returns all active documents uploaded by the authenticated citizen")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getMyDocuments(
            @RequestParam @Parameter(description = "Auth User ID (JWT subject)") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Citizen documents retrieved", documentService.getMyDocuments(authUserId)));
    }

    // ─── VERIFY ──────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/verify")
    @Operation(summary = "Verify Document", description = "Officer endpoint: approves and verifies a document. Triggers notification callback.")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @PathVariable String id,
            @Valid @RequestBody DocumentVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Document verified successfully", documentService.verifyDocument(id, request)));
    }

    // ─── REJECT ──────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject Document", description = "Officer endpoint: rejects a document with reason. Triggers notification callback.")
    public ResponseEntity<ApiResponse<DocumentResponse>> rejectDocument(
            @PathVariable String id,
            @Valid @RequestBody DocumentVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Document rejected", documentService.rejectDocument(id, request)));
    }

    // ─── DELETE (SOFT DELETE) ────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Document", description = "Soft-deletes a document entry and removes the physical file from storage")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String id,
            @RequestParam String authUserId) {
        documentService.deleteDocument(id, authUserId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }

    // ─── READINESS ───────────────────────────────────────────────────────────────

    @GetMapping("/readiness")
    @Operation(summary = "Get Document Readiness", description = "Returns document readiness statistics and missing essential documents for Citizen Service integration")
    public ResponseEntity<ApiResponse<DocumentReadinessResponse>> getDocumentReadiness(
            @RequestParam String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Document readiness calculated", documentService.calculateDocumentReadiness(authUserId)));
    }
}
