package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.GrievancePriority;
import com.schemebridge.scheme.document.GrievanceStatus;
import com.schemebridge.scheme.document.GrievanceTimelineEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrievanceResponse {
    private String id;
    private String grievanceNumber;
    private String userId;
    private String applicationId;
    private String schemeCode;
    private String category;
    private String subject;
    private String description;
    private GrievancePriority priority;
    private GrievanceStatus status;
    private String assignedTo;
    private String resolution;
    private Instant resolvedAt;
    private List<GrievanceTimelineEntry> timeline;
    private Instant createdAt;
    private Instant updatedAt;
}
