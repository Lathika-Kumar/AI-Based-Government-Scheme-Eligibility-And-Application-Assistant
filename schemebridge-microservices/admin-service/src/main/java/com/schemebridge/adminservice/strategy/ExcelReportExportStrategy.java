package com.schemebridge.adminservice.strategy;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ExcelReportExportStrategy implements ReportExportStrategy {

    @Override
    public boolean supports(ReportFormat format) {
        return ReportFormat.EXCEL.equals(format);
    }

    @Override
    public byte[] exportReport(ReportType reportType, String reportName) {
        log.info("Exporting Excel Report: {} (Type: {})", reportName, reportType);
        String excelContent = "ReportTitle,Type,Timestamp\n" + reportName + "," + reportType + "," + java.time.Instant.now() + "\n";
        return excelContent.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getFormatExtension() {
        return ".xlsx";
    }
}
