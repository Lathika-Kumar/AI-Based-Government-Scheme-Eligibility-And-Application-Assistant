package com.schemebridge.applicationservice.document;

import com.schemebridge.applicationservice.enums.ApplicationStage;
import com.schemebridge.applicationservice.enums.ApplicationStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "idx_auth_scheme", def = "{'authUserId': 1, 'schemeId': 1}"),
    @CompoundIndex(name = "idx_auth_status", def = "{'authUserId': 1, 'applicationStatus': 1}"),
    @CompoundIndex(name = "idx_dept_status", def = "{'departmentId': 1, 'applicationStatus': 1}")
})
public class ApplicationDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String applicationNumber; // SB-APP-YYYY-000001

    @Indexed
    private String authUserId;
    private String citizenProfileId;

    @Indexed
    private String schemeId;
    private String schemeCode;
    private String schemeName;

    private String departmentId;
    private String departmentName;

    @Indexed
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

    @Builder.Default
    private List<TrackingHistoryItem> trackingHistory = new ArrayList<>();

    @Builder.Default
    private List<RequiredDocumentRef> requiredDocuments = new ArrayList<>();

    @Builder.Default
    private List<UploadedDocumentRef> uploadedDocuments = new ArrayList<>();

    private EligibilitySnapshot eligibilitySnapshot;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    private boolean active;
}
