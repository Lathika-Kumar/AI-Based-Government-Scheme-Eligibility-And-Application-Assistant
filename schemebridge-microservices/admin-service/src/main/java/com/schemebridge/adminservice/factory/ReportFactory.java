package com.schemebridge.adminservice.factory;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.entity.ReportHistory;
import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory Pattern implementation for constructing ReportHistory domain entities.
 */
@Component
public class ReportFactory {

    public ReportHistory createReportHistory(String reportName, ReportType reportType, ReportFormat format, String generatedBy, String filePath, long fileSizeBytes) {
        return ReportHistory.builder()
                .reportId(AdminConstants.REPORT_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase())
                .reportName(reportName != null ? reportName : reportType.name() + " Report")
                .reportType(reportType)
                .reportFormat(format)
                .generatedBy(generatedBy != null ? generatedBy : AdminConstants.SYSTEM_ACTOR)
                .filePath(filePath)
                .fileSizeBytes(fileSizeBytes)
                .generatedAt(Instant.now())
                .build();
    }
}
