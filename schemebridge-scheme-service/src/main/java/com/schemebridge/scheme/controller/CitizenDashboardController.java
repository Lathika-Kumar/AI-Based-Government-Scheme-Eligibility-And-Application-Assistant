package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.CitizenDashboardResponse;
import com.schemebridge.scheme.service.CitizenDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/dashboard", "/api/citizen/dashboard"})
@RequiredArgsConstructor
@Tag(name = "Citizen Dashboard", description = "Endpoints for aggregated citizen dashboard summary and action brief")
@SecurityRequirement(name = "BearerAuth")
public class CitizenDashboardController {

    private final CitizenDashboardService citizenDashboardService;

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("Unauthorized request");
        }
        return auth.getName();
    }

    @GetMapping({"", "/summary"})
    @Operation(summary = "Get aggregated citizen dashboard summary, metrics, and daily action brief")
    public ResponseEntity<CitizenDashboardResponse> getDashboardSummary() {
        String userId = getUserId();
        CitizenDashboardResponse response = citizenDashboardService.getCitizenDashboardSummary(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
