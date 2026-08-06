package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.DocumentMetadataResponse;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.dto.DocumentUploadRequest;
import com.schemebridge.dto.DocumentVerificationRequest;
import com.schemebridge.dto.VaultScoreResponse;
import com.schemebridge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "Upload document", description = "Uploads a new document file and metadata record into the citizen vault")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "issuer", required = false) String issuer,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            @RequestParam(value = "source", required = false) String source) {

        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .documentType(documentType)
                .documentName(documentName)
                .remarks(remarks)
                .issuer(issuer)
                .expiryDate(expiryDate)
                .source(source)
                .build();

        DocumentResponse response = documentService.uploadDocument(userDetails.getUsername(), file, request);
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'VERIFICATION_OFFICER')")
    @Operation(summary = "Verify document", description = "Marks a document as verified after checks")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DocumentVerificationRequest request) {
        DocumentResponse response = documentService.verifyDocument(id, userDetails.getUsername(), request);
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

    @GetMapping("/vault-score")
    @Operation(summary = "Get vault score", description = "Calculates document completeness for the authenticated citizen")
    public ResponseEntity<ApiResponse<VaultScoreResponse>> getVaultScore(@AuthenticationPrincipal UserDetails userDetails) {
        VaultScoreResponse response = documentService.getVaultScore(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Vault score retrieved successfully", response));
    }

    @PostMapping("/digilocker/sync")
    @Operation(summary = "Sync DigiLocker", description = "Simulates the DigiLocker metadata synchronization for the authenticated citizen")
    public ResponseEntity<ApiResponse<DocumentMetadataResponse>> syncDigilocker(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DocumentMetadataResponse request) {
        DocumentMetadataResponse response = documentService.syncDigilocker(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("DigiLocker sync completed successfully", response));
    }
}
