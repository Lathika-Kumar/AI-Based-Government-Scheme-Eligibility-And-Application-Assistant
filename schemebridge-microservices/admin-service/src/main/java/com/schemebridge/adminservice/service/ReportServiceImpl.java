package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.ReportGenerateRequest;
import com.schemebridge.adminservice.dto.ReportResponse;
import com.schemebridge.adminservice.entity.ReportHistory;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.enums.ReportType;
import com.schemebridge.adminservice.factory.ReportFactory;
import com.schemebridge.adminservice.repository.ReportHistoryRepository;
import com.schemebridge.adminservice.strategy.ReportExportStrategy;
import com.schemebridge.adminservice.strategy.ReportExportStrategyFactory;
import com.schemebridge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportHistoryRepository reportRepository;
    private final ReportExportStrategyFactory strategyFactory;
    private final ReportFactory reportFactory;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ReportResponse generateReport(String actorEmail, ReportGenerateRequest request) {
        log.info("Generating report: Type={}, Format={} by {}", request.getReportType(), request.getReportFormat(), actorEmail);

        ReportExportStrategy strategy = strategyFactory.getStrategy(request.getReportFormat());
        byte[] content = strategy.exportReport(request.getReportType(), request.getReportName());

        String reportName = request.getReportName() != null ? request.getReportName() : request.getReportType().name() + "_Report";
        String filePath = "/exports/reports/" + reportName.replaceAll("\\s+", "_") + strategy.getFormatExtension();

        ReportHistory history = reportFactory.createReportHistory(reportName, request.getReportType(), request.getReportFormat(), actorEmail, filePath, content.length);
        ReportHistory saved = reportRepository.save(history);

        auditLogService.logActivity(actorEmail, AdminActionType.REPORT_GENERATED, "Report", saved.getReportId(),
                "Generated " + request.getReportFormat() + " report: " + reportName);

        return mapToResponse(saved);
    }

    @Override
    public ReportResponse getReportById(String reportId) {
        return mapToResponse(findEntity(reportId));
    }

    @Override
    public Page<ReportResponse> getReportHistory(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return reportRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<ReportResponse> getReportsByType(ReportType reportType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AdminConstants.DEFAULT_SORT_BY));
        return reportRepository.findByReportTypeAndActiveTrue(reportType, pageable).map(this::mapToResponse);
    }

    @Override
    public byte[] downloadReport(String reportId) {
        ReportHistory history = findEntity(reportId);
        ReportExportStrategy strategy = strategyFactory.getStrategy(history.getReportFormat());
        return strategy.exportReport(history.getReportType(), history.getReportName());
    }

    private ReportHistory findEntity(String reportId) {
        return reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(AdminConstants.ERR_REPORT_NOT_FOUND + reportId));
    }

    private ReportResponse mapToResponse(ReportHistory r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reportId(r.getReportId())
                .reportName(r.getReportName())
                .reportType(r.getReportType())
                .reportFormat(r.getReportFormat())
                .generatedBy(r.getGeneratedBy())
                .filePath(r.getFilePath())
                .fileSizeBytes(r.getFileSizeBytes())
                .generatedAt(r.getGeneratedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
