package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.AdminDashboardResponse;
import com.schemebridge.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER', 'VERIFICATION_OFFICER')")
@Tag(name = "Admin Dashboard", description = "Administrative dashboard metrics and officer workflow views")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard metrics", description = "Returns aggregated metrics for admin, scheme manager, and verification officer dashboards")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = adminDashboardService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard retrieved successfully", response));
    }
}
