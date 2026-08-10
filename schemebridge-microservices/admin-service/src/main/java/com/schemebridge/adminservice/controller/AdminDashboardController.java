package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.dto.AdminDashboardResponse;
import com.schemebridge.adminservice.service.DashboardService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard & Analytics", description = "Central KPI summary, registration statistics, scheme status, and audit metrics APIs")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get Admin Dashboard Summary", description = "Retrieves aggregated KPIs, citizen counts, application status counts, officer metrics, and recent activities")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard summary retrieved successfully", dashboardService.getDashboardSummary()));
    }
}
