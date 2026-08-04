package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.DocumentRequest;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Document Vault", description = "Citizen document vault endpoints for upload, list, verification, and retrieval")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List documents", description = "Returns documents uploaded by the authenticated citizen")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocuments(@AuthenticationPrincipal UserDetails userDetails) {
        List<DocumentResponse> documents = documentService.getDocuments(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", documents));
    }

    @PostMapping
    @Operation(summary = "Upload document", description = "Uploads a new document metadata record into the citizen vault")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentRequest request) {
        DocumentResponse response = documentService.uploadDocument(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document uploaded successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Soft deletes the citizen document by ID")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify document", description = "Marks a document as verified after checks")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentResponse response = documentService.verifyDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document verified successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Retrieves a single document record for the authenticated citizen")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        DocumentResponse response = documentService.getDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Document retrieved successfully", response));
    }
}
