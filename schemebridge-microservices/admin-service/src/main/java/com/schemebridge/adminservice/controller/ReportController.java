package com.schemebridge.adminservice.controller;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.ReportGenerateRequest;
import com.schemebridge.adminservice.dto.ReportResponse;
import com.schemebridge.adminservice.enums.ReportType;
import com.schemebridge.adminservice.service.ReportService;
import com.schemebridge.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Report Engine & Analytics Export", description = "PDF, Excel & CSV Analytics Report Generation, Download & History APIs")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @Operation(summary = "Generate Report", description = "Generates PDF, Excel, or CSV report for platform analytics")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @RequestHeader(value = AdminConstants.HEADER_USER_EMAIL, required = false, defaultValue = "admin@schemebridge.gov.in") String actorEmail,
            @Valid @RequestBody ReportGenerateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report generated successfully", reportService.generateReport(actorEmail, request)));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get Report Metadata", description = "Retrieves generated report metadata by report ID")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(@PathVariable String reportId) {
        return ResponseEntity.ok(ApiResponse.success("Report metadata retrieved", reportService.getReportById(reportId)));
    }

    @GetMapping
    @Operation(summary = "Get Report History", description = "Retrieves paginated report history")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReportHistory(
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AdminConstants.DEFAULT_PAGE_SIZE) int size) {
        Page<ReportResponse> result = (type != null) ? reportService.getReportsByType(type, page, size) : reportService.getReportHistory(page, size);
        return ResponseEntity.ok(ApiResponse.success("Report history retrieved", result));
    }

    @GetMapping("/{reportId}/download")
    @Operation(summary = "Download Report Content", description = "Downloads raw report file byte payload")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String reportId) {
        byte[] fileBytes = reportService.downloadReport(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + reportId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }
}
