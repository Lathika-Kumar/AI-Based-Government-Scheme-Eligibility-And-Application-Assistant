package com.schemebridge.applicationservice.controller;

import com.schemebridge.applicationservice.dto.*;
import com.schemebridge.applicationservice.service.ApplicationService;
import com.schemebridge.applicationservice.service.ApplicationTimelineService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Application Lifecycle", description = "Government scheme application management APIs – create, submit, withdraw, track, search, and dashboard")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationTimelineService timelineService;

    // ─── CREATE ──────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create Application",
               description = "Creates a new scheme application in DRAFT status. " +
                             "Rejects if an active application already exists for the same citizen+scheme combination.")
    public ResponseEntity<ApiResponse<ApplicationResponse>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully in DRAFT status", response));
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get Application By ID",
               description = "Retrieves a complete application document including tracking history, eligibility snapshot, and documents")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable @Parameter(description = "MongoDB application document ID") String id) {
        return ResponseEntity.ok(ApiResponse.success("Application retrieved successfully",
                applicationService.getApplicationById(id)));
    }

    // ─── GET MY APPLICATIONS ─────────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(summary = "Get My Applications",
               description = "Returns all applications for a specific citizen, paginated and sorted by most recent")
    public ResponseEntity<ApiResponse<Page<ApplicationSummaryResponse>>> getMyApplications(
            @RequestParam @Parameter(description = "Auth User ID (JWT subject)") String authUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ApplicationSummaryResponse> result = applicationService.getMyApplications(authUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", result));
    }

    // ─── DASHBOARD ───────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Application Dashboard",
               description = "Returns aggregated application statistics for a citizen: total, approved, pending, rejected, benefit released, and 5 most recent applications")
    public ResponseEntity<ApiResponse<ApplicationDashboardResponse>> getDashboard(
            @RequestParam @Parameter(description = "Auth User ID") String authUserId) {
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully",
                applicationService.getDashboard(authUserId)));
    }

    // ─── UPDATE (DRAFT ONLY) ─────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update Application",
               description = "Updates a DRAFT application's remarks or scheme details. Only DRAFT status applications can be updated.")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplication(
            @PathVariable String id,
            @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated successfully",
                applicationService.updateApplication(id, request)));
    }

    // ─── SUBMIT ──────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit Application",
               description = "Transitions a DRAFT application to SUBMITTED status, moving it to the document verification stage. Triggers notification to citizen.")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully",
                applicationService.submitApplication(id)));
    }

    // ─── WITHDRAW ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw Application",
               description = "Withdraws an active application. Cannot withdraw terminal applications (APPROVED, REJECTED, BENEFIT_RELEASED). Triggers withdrawal notification.")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdrawApplication(
            @PathVariable String id,
            @RequestBody(required = false) WithdrawRequest request) {
        WithdrawRequest withdrawRequest = request != null ? request : new WithdrawRequest();
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully",
                applicationService.withdrawApplication(id, withdrawRequest)));
    }

    // ─── TIMELINE ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Get Application Timeline",
               description = "Returns the complete chronological tracking history of an application – every status change, who made it, when, and remarks")
    public ResponseEntity<ApiResponse<ApplicationTimelineResponse>> getTimeline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Application timeline retrieved successfully",
                timelineService.getTimeline(id)));
    }

    // ─── SEARCH ──────────────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search Applications",
               description = "Searches applications for a citizen by keyword matching against applicationNumber, schemeName, and departmentName")
    public ResponseEntity<ApiResponse<Page<ApplicationSummaryResponse>>> searchApplications(
            @RequestParam @Parameter(description = "Auth User ID") String authUserId,
            @RequestParam(defaultValue = "") @Parameter(description = "Search keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ApplicationSummaryResponse> result = applicationService.searchApplications(authUserId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
    }
}
