package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.DocumentOcrResponse;
import com.schemebridge.scheme.service.DocumentOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Document OCR Intelligence & Field Extraction.
 */
@RestController
@RequestMapping("/api/applications/{applicationId}/documents/{documentCode}/ocr")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Document OCR & Intelligence", description = "Endpoints for processing and retrieving document OCR data")
@SecurityRequirement(name = "BearerAuth")
public class DocumentOcrController {

    private final DocumentOcrService documentOcrService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Unauthorized request");
        }
        return auth.getName();
    }

    private boolean isPrivileged() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                a.getAuthority().equals("ROLE_SCHEME_MANAGER") ||
                a.getAuthority().equals("ROLE_VERIFICATION_OFFICER"));
    }

    @PostMapping
    @Operation(summary = "Process or retrieve OCR text & extracted fields for an uploaded document")
    public ResponseEntity<DocumentOcrResponse> processOcr(
            @PathVariable String applicationId,
            @PathVariable String documentCode
    ) {
        String userId = getUserId();
        boolean privileged = isPrivileged();
        DocumentOcrResponse response = documentOcrService.processOcr(applicationId, documentCode, userId, privileged);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get existing OCR extraction result for an uploaded document")
    public ResponseEntity<DocumentOcrResponse> getOcrResult(
            @PathVariable String applicationId,
            @PathVariable String documentCode
    ) {
        String userId = getUserId();
        boolean privileged = isPrivileged();
        DocumentOcrResponse response = documentOcrService.getOcrResult(applicationId, documentCode, userId, privileged);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
