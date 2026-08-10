package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long id;
    private String reportId;
    private String reportName;
    private ReportType reportType;
    private ReportFormat reportFormat;
    private String generatedBy;
    private String filePath;
    private Long fileSizeBytes;
    private Instant generatedAt;
    private Instant createdAt;
}
