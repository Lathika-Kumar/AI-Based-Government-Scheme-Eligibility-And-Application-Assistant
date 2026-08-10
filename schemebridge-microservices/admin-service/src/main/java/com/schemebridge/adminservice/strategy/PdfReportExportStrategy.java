package com.schemebridge.adminservice.strategy;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class PdfReportExportStrategy implements ReportExportStrategy {

    @Override
    public boolean supports(ReportFormat format) {
        return ReportFormat.PDF.equals(format);
    }

    @Override
    public byte[] exportReport(ReportType reportType, String reportName) {
        log.info("Exporting PDF Report: {} (Type: {})", reportName, reportType);
        String pdfContent = "%PDF-1.4\n1 0 obj\n<< /Title (" + reportName + ") /ReportType (" + reportType + ") >>\nendobj\n";
        return pdfContent.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getFormatExtension() {
        return ".pdf";
    }
}
