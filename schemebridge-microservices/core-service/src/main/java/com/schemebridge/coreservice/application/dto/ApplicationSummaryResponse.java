package com.schemebridge.coreservice.application.dto;

import com.schemebridge.coreservice.application.enums.ApplicationStage;
import com.schemebridge.coreservice.application.enums.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSummaryResponse {
    private String id;
    private String applicationNumber;
    private String schemeCode;
    private String schemeName;
    private String departmentName;
    private ApplicationStatus applicationStatus;
    private ApplicationStage applicationStage;
    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
}
