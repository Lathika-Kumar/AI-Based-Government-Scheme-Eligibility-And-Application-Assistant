package com.schemebridge.applicationservice.dto;

import com.schemebridge.applicationservice.document.EligibilitySnapshot;
import com.schemebridge.applicationservice.document.TrackingHistoryItem;
import com.schemebridge.applicationservice.document.UploadedDocumentRef;
import com.schemebridge.applicationservice.document.RequiredDocumentRef;
import com.schemebridge.applicationservice.enums.ApplicationStage;
import com.schemebridge.applicationservice.enums.ApplicationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private String id;
    private String applicationNumber;
    private String authUserId;
    private String citizenProfileId;
    private String schemeId;
    private String schemeCode;
    private String schemeName;
    private String departmentId;
    private String departmentName;
    private ApplicationStatus applicationStatus;
    private ApplicationStage applicationStage;
    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private BigDecimal benefitAmount;
    private String benefitType;
    private String remarks;
    private String rejectionReason;
    private String assignedOfficerId;
    private String assignedOfficerName;
    private List<TrackingHistoryItem> trackingHistory;
    private List<RequiredDocumentRef> requiredDocuments;
    private List<UploadedDocumentRef> uploadedDocuments;
    private EligibilitySnapshot eligibilitySnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
