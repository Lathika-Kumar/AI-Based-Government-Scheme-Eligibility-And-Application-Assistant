package com.schemebridge.dto;

import com.schemebridge.enums.GrievanceCategory;
import com.schemebridge.enums.GrievanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrievanceResponse {
    private String id;
    private String userId;
    private String subject;
    private String description;
    private GrievanceCategory category;
    private GrievanceStatus status;
    private String resolutionNotes;
    private Instant createdAt;
    private Instant resolvedAt;
}
