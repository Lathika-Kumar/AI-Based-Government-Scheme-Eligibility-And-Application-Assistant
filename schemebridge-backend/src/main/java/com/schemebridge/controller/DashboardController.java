package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.DashboardSummaryResponse;
import com.schemebridge.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints for citizen dashboard summary metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get citizen dashboard summary", description = "Retrieves aggregated metrics including eligible schemes count, application status counts, and completion scores for fast dashboard loading")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(@AuthenticationPrincipal UserDetails userDetails) {
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", response));
    }
}
