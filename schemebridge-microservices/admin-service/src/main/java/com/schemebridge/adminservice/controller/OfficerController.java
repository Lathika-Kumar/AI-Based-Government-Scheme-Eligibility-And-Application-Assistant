package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.OfficerRequest;
import com.schemebridge.adminservice.dto.OfficerResponse;
import com.schemebridge.adminservice.dto.OfficerStatusUpdateRequest;
import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.service.OfficerService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/officers")
@RequiredArgsConstructor
@Tag(name = "Officer Management", description = "Officer CRUD, Role assignments, Department jurisdiction, and Status management APIs")
public class OfficerController {

    private final OfficerService officerService;

    @PostMapping
    @Operation(summary = "Create Officer", description = "Creates a new government verification or support officer profile")
    public ResponseEntity<ApiResponse<OfficerResponse>> createOfficer(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @Valid @RequestBody OfficerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Officer created successfully", officerService.createOfficer(actorEmail, request)));
    }

    @PutMapping("/{officerId}")
    @Operation(summary = "Update Officer Details", description = "Updates an existing officer profile and role assignments")
    public ResponseEntity<ApiResponse<OfficerResponse>> updateOfficer(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String officerId,
            @Valid @RequestBody OfficerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Officer details updated successfully", officerService.updateOfficer(actorEmail, officerId, request)));
    }

    @PutMapping("/{officerId}/status")
    @Operation(summary = "Update Officer Status", description = "Changes officer status (ACTIVE, SUSPENDED, DEACTIVATED)")
    public ResponseEntity<ApiResponse<OfficerResponse>> updateOfficerStatus(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String officerId,
            @Valid @RequestBody OfficerStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Officer status updated", officerService.updateOfficerStatus(actorEmail, officerId, request)));
    }

    @GetMapping("/{officerId}")
    @Operation(summary = "Get Officer Details", description = "Retrieves officer profile by unique officer ID")
    public ResponseEntity<ApiResponse<OfficerResponse>> getOfficerById(@PathVariable String officerId) {
        return ResponseEntity.ok(ApiResponse.success("Officer details retrieved", officerService.getOfficerById(officerId)));
    }

    @GetMapping
    @Operation(summary = "List Officers (Paginated)", description = "Retrieves paginated list of officers with optional role filtering")
    public ResponseEntity<ApiResponse<Page<OfficerResponse>>> getAllOfficers(
            @RequestParam(required = false) OfficerRole role,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        Page<OfficerResponse> result = (role != null) ? officerService.getOfficersByRole(role, page, size) : officerService.getAllOfficers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Officers list retrieved", result));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Officers", description = "Searches officers by name or email query")
    public ResponseEntity<ApiResponse<Page<OfficerResponse>>> searchOfficers(
            @RequestParam String q,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success("Officer search results retrieved", officerService.searchOfficers(q, page, size)));
    }

    @DeleteMapping("/{officerId}")
    @Operation(summary = "Delete / Deactivate Officer", description = "Soft-deletes an officer profile")
    public ResponseEntity<ApiResponse<String>> deleteOfficer(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @PathVariable String officerId) {
        officerService.deleteOfficer(actorEmail, officerId);
        return ResponseEntity.ok(ApiResponse.success("Officer deactivated successfully", officerId));
    }
}
