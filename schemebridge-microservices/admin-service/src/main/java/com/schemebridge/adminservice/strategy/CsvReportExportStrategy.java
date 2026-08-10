package com.schemebridge.adminservice.strategy;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CsvReportExportStrategy implements ReportExportStrategy {

    @Override
    public boolean supports(ReportFormat format) {
        return ReportFormat.CSV.equals(format);
    }

    @Override
    public byte[] exportReport(ReportType reportType, String reportName) {
        log.info("Exporting CSV Report: {} (Type: {})", reportName, reportType);
        String csvContent = "Report Name,Report Type,Generated At\n" + reportName + "," + reportType + "," + java.time.Instant.now() + "\n";
        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getFormatExtension() {
        return ".csv";
    }
}
