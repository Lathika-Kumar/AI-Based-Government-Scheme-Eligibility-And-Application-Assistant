package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.service.SchemeService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/schemes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER')")
@Tag(name = "Admin Schemes Management", description = "Secured administrative endpoints for scheme CRUD, publishing, and archiving")
public class AdminSchemeController {

    private final SchemeService schemeService;

    @PostMapping
    @Operation(summary = "Create scheme", description = "Creates a new government scheme document in DRAFT status and logs audit record")
    public ResponseEntity<ApiResponse<SchemeResponse>> createScheme(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SchemeRequest request) {
        SchemeResponse response = schemeService.createScheme(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Scheme created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update scheme", description = "Updates scheme fields, increments version, and records audit log")
    public ResponseEntity<ApiResponse<SchemeResponse>> updateScheme(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SchemeRequest request) {
        SchemeResponse response = schemeService.updateScheme(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Scheme updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete scheme", description = "Marks scheme as ARCHIVED and sets soft-delete flags")
    public ResponseEntity<ApiResponse<Void>> softDeleteScheme(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        schemeService.softDeleteScheme(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Scheme archived and soft-deleted successfully", null));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish scheme", description = "Transitions scheme status to PUBLISHED making it visible to citizens")
    public ResponseEntity<ApiResponse<SchemeResponse>> publishScheme(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        SchemeResponse response = schemeService.publishScheme(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Scheme published successfully", response));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview scheme", description = "Allows administrators to preview scheme details regardless of status")
    public ResponseEntity<ApiResponse<SchemeResponse>> getSchemePreview(@PathVariable String id) {
        SchemeResponse response = schemeService.getSchemePreview(id);
        return ResponseEntity.ok(ApiResponse.success("Scheme preview retrieved successfully", response));
    }
}
