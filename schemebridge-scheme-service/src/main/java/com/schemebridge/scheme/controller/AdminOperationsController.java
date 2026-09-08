package com.schemebridge.scheme.controller;

import com.schemebridge.scheme.dto.response.AdminMetricsResponse;
import com.schemebridge.scheme.service.AdminAnalyticsService;
import com.schemebridge.scheme.service.AdminMetricsService;
import com.schemebridge.scheme.service.AdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations & Analytics", description = "Endpoints for administrative metrics, analytics, and operational reporting")
@SecurityRequirement(name = "BearerAuth")
public class AdminOperationsController {

    private final AdminMetricsService adminMetricsService;
    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminReportService adminReportService;

    @GetMapping("/metrics")
    @Operation(summary = "Get aggregated operations dashboard metrics across schemes, applications, and documents")
    public ResponseEntity<AdminMetricsResponse> getMetrics() {
        AdminMetricsResponse response = adminMetricsService.getMetrics();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get detailed analytics data including approval rates and intake timelines")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        Map<String, Object> response = adminAnalyticsService.getAnalyticsData();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = {"/reports/applications", "/reports/applications/export"}, produces = "text/csv")
    @Operation(summary = "Export applications report in CSV format")
    public ResponseEntity<String> exportApplicationsReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schemeCode) {
        String csv = adminReportService.generateApplicationsCsv(status, schemeCode);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications_report.csv");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv);
    }

    @GetMapping(value = {"/reports/schemes", "/reports/schemes/export"}, produces = "text/csv")
    @Operation(summary = "Export schemes catalog report in CSV format")
    public ResponseEntity<String> exportSchemesReport(@RequestParam(required = false) String status) {
        String csv = adminReportService.generateSchemesCsv(status);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schemes_report.csv");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv);
    }

    @GetMapping(value = {"/reports/grievances", "/reports/grievances/export"}, produces = "text/csv")
    @Operation(summary = "Export citizen grievances report in CSV format")
    public ResponseEntity<String> exportGrievancesReport(@RequestParam(required = false) String status) {
        String csv = adminReportService.generateGrievancesCsv(status);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grievances_report.csv");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType("text/csv")).body(csv);
    }
}
