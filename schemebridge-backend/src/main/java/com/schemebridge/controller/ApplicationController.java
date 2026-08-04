package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.ApplicationRequest;
import com.schemebridge.dto.ApplicationResponse;
import com.schemebridge.service.ApplicationService;
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
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Citizen application processing endpoints and saved scheme management")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List applications", description = "Returns all applications submitted by the authenticated citizen")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplications(@AuthenticationPrincipal UserDetails userDetails) {
        List<ApplicationResponse> applications = applicationService.getApplications(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", applications));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application", description = "Retrieves a single application record by ID for the authenticated citizen")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApplicationResponse application = applicationService.getApplicationById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Application retrieved successfully", application));
    }

    @PostMapping
    @Operation(summary = "Submit application", description = "Submits a new scheme application for the authenticated citizen")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submitApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.submitApplication(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Application submitted successfully", response));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw application", description = "Withdraws an application by ID")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        applicationService.withdrawApplication(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", null));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved schemes", description = "Returns scheme IDs saved by the authenticated citizen")
    public ResponseEntity<ApiResponse<List<String>>> getSavedSchemes(@AuthenticationPrincipal UserDetails userDetails) {
        List<String> savedSchemes = applicationService.getSavedSchemes(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Saved schemes retrieved successfully", savedSchemes));
    }

    @PostMapping("/saved/{schemeId}")
    @Operation(summary = "Save scheme", description = "Marks a scheme as saved for the authenticated citizen")
    public ResponseEntity<ApiResponse<Void>> saveScheme(
            @PathVariable String schemeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        applicationService.saveScheme(userDetails.getUsername(), schemeId);
        return ResponseEntity.ok(ApiResponse.success("Scheme saved successfully", null));
    }

    @DeleteMapping("/saved/{schemeId}")
    @Operation(summary = "Unsave scheme", description = "Removes a scheme from the citizen's saved list")
    public ResponseEntity<ApiResponse<Void>> unsaveScheme(
            @PathVariable String schemeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        applicationService.unsaveScheme(userDetails.getUsername(), schemeId);
        return ResponseEntity.ok(ApiResponse.success("Scheme unsaved successfully", null));
    }
}
