package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.GrievanceRequest;
import com.schemebridge.dto.GrievanceResponse;
import com.schemebridge.service.GrievanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Grievance Management", description = "Citizen and admin grievance lifecycle endpoints")
public class GrievanceController {

    private final GrievanceService grievanceService;

    @PostMapping("/grievances")
    @Operation(summary = "Create grievance", description = "Creates a grievance ticket for the authenticated citizen")
    public ResponseEntity<ApiResponse<GrievanceResponse>> createGrievance(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GrievanceRequest request) {
        GrievanceResponse response = grievanceService.createGrievance(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Grievance created successfully", response));
    }

    @GetMapping("/grievances")
    @Operation(summary = "List grievances", description = "Lists all grievances raised by the authenticated citizen")
    public ResponseEntity<ApiResponse<List<GrievanceResponse>>> getGrievances(@AuthenticationPrincipal UserDetails userDetails) {
        List<GrievanceResponse> grievances = grievanceService.getGrievances(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Grievances retrieved successfully", grievances));
    }

    @GetMapping("/admin/grievances")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER', 'VERIFICATION_OFFICER')")
    @Operation(summary = "List all grievances", description = "Returns all grievances for admin and officer review")
    public ResponseEntity<ApiResponse<List<GrievanceResponse>>> getAllGrievancesForAdmin() {
        List<GrievanceResponse> grievances = grievanceService.getAllGrievancesForAdmin();
        return ResponseEntity.ok(ApiResponse.success("Admin grievances retrieved successfully", grievances));
    }

    @PostMapping("/admin/grievances/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER', 'VERIFICATION_OFFICER')")
    @Operation(summary = "Resolve grievance", description = "Marks a grievance as resolved with officer notes")
    public ResponseEntity<ApiResponse<GrievanceResponse>> resolveGrievance(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String resolutionNotes) {
        GrievanceResponse response = grievanceService.resolveGrievance(id, userDetails.getUsername(), resolutionNotes);
        return ResponseEntity.ok(ApiResponse.success("Grievance resolved successfully", response));
    }
}
