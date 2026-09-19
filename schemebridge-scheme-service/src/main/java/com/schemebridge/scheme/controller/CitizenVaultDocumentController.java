package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.document.CitizenVaultDocument;
import com.schemebridge.scheme.dto.response.DocumentDownloadDto;
import com.schemebridge.scheme.service.CitizenVaultDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents/vault")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Citizen Document Vault", description = "Endpoints for citizen permanent persistent document repository")
@SecurityRequirement(name = "BearerAuth")
public class CitizenVaultDocumentController {

    private final CitizenVaultDocumentService vaultDocumentService;
    private final com.schemebridge.scheme.service.DocumentExtractionService documentExtractionService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !StringUtils.hasText(auth.getName())) {
            throw new SecurityException("Authentication required.");
        }
        return auth.getName();
    }

    private boolean isPrivilegedUser(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority().toUpperCase().replace("ROLE_", "");
            return role.equals("ADMIN") || role.equals("ADMINISTRATOR") ||
                   role.equals("SCHEME_MANAGER") || role.equals("VERIFICATION_OFFICER") ||
                   role.equals("SUPER_ADMIN");
        });
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Extract structured document attributes and verify identity against authenticated citizen")
    public ResponseEntity<com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse> extractVaultDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentCode", required = false) String documentCode,
            @RequestParam(value = "documentType", required = false) String documentType
    ) {
        String userId = getUserId();
        com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse response =
                documentExtractionService.extractAndVerify(file, documentCode, documentType, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all persistent documents in the authenticated citizen's Document Vault")
    public ResponseEntity<List<CitizenVaultDocument>> getVaultDocuments() {
        String userId = getUserId();
        List<CitizenVaultDocument> docs = vaultDocumentService.getVaultDocuments(userId);
        return ResponseEntity.ok(docs);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a document into citizen's permanent Document Vault (PDF, JPG, PNG under 10MB)")
    public ResponseEntity<CitizenVaultDocument> uploadVaultDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentCode", required = false) String documentCode,
            @RequestParam(value = "type", required = false) String documentType,
            @RequestParam(value = "name", required = false) String documentName,
            @RequestParam(value = "issuer", required = false) String issuer,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            @RequestParam(value = "holderName", required = false) String holderName,
            @RequestParam(value = "docNumber", required = false) String docNumber,
            @RequestParam(value = "source", required = false) String source
    ) {
        String userId = getUserId();
        CitizenVaultDocument saved = vaultDocumentService.uploadVaultDocument(
                userId,
                documentCode,
                documentType,
                documentName,
                issuer,
                expiryDate,
                holderName,
                docNumber,
                source,
                file
        );
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get details of a specific document from citizen's vault")
    public ResponseEntity<CitizenVaultDocument> getVaultDocument(@PathVariable String documentId) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = isPrivilegedUser(auth);

        return vaultDocumentService.getVaultDocumentById(documentId, userId, isPrivileged)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download binary document from citizen's permanent Document Vault")
    public ResponseEntity<InputStreamResource> downloadVaultDocument(@PathVariable String documentId) {
        String userId = getUserId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isPrivileged = isPrivilegedUser(auth);

        DocumentDownloadDto downloadDto = vaultDocumentService.getVaultDocumentDownload(documentId, userId, isPrivileged);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + downloadDto.getFileName() + "\"");
        headers.setContentType(MediaType.parseMediaType(downloadDto.getContentType()));
        if (downloadDto.getFileSize() != null && downloadDto.getFileSize() > 0) {
            headers.setContentLength(downloadDto.getFileSize());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(downloadDto.getInputStream()));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a document from citizen's Document Vault")
    public ResponseEntity<Map<String, Object>> deleteVaultDocument(@PathVariable String documentId) {
        String userId = getUserId();
        vaultDocumentService.deleteVaultDocument(documentId, userId);
        return ResponseEntity.ok(Map.of("success", true, "id", documentId));
    }
}
