package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationReviewResponse {
    private String id;
    private String applicationId;
    private String reviewerId;
    private String reviewerRole;
    private String reviewStatus;
    private String remarks;
    private String decisionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
