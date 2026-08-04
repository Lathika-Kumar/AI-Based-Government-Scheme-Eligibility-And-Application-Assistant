package com.schemebridge.dto;

import com.schemebridge.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private String id;
    private String userId;
    private String schemeId;
    private String schemeName;
    private String ministry;
    private String referenceNumber;
    private String remarks;
    private List<String> documents;
    private List<ApplicationTimelineEntryResponse> timeline;
    private ApplicationStatus status;
    private Instant submittedAt;
    private Instant withdrawnAt;
    private Instant createdAt;
    private Instant updatedAt;
}
