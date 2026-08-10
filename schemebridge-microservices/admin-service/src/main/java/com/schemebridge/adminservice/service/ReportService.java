package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.ReportGenerateRequest;
import com.schemebridge.adminservice.dto.ReportResponse;
import com.schemebridge.adminservice.enums.ReportType;
import org.springframework.data.domain.Page;

public interface ReportService {

    ReportResponse generateReport(String actorEmail, ReportGenerateRequest request);

    ReportResponse getReportById(String reportId);

    Page<ReportResponse> getReportHistory(int page, int size);

    Page<ReportResponse> getReportsByType(ReportType reportType, int page, int size);

    byte[] downloadReport(String reportId);
}
