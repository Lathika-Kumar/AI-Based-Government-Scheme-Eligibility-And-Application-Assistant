package com.schemebridge.coreservice.application.dto;

import com.schemebridge.coreservice.application.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusUpdateRequest {
    private ApplicationStatus status;
    private String remarks;
    private String rejectionReason;
    private String benefitDetails;
}
