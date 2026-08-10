package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.ReportFormat;
import com.schemebridge.adminservice.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGenerateRequest {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Report format is required")
    private ReportFormat reportFormat;

    private String reportName;
}
