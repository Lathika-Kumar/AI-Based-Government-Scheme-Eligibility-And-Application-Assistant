package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.FeedbackStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {

    private Long id;
    private String feedbackId;
    private String userId;
    private String userEmail;
    private String subject;
    private String message;
    private Integer rating;
    private FeedbackStatus status;
    private String assignedOfficerId;
    private String resolutionNotes;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
