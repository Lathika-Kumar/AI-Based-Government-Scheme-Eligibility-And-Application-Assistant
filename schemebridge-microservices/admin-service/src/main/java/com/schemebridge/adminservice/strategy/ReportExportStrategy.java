package com.schemebridge.adminservice.strategy;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;

/**
 * Strategy Pattern interface defining report generation and export contracts.
 */
public interface ReportExportStrategy {

    boolean supports(ReportFormat format);

    byte[] exportReport(ReportType reportType, String reportName);

    String getFormatExtension();
}
