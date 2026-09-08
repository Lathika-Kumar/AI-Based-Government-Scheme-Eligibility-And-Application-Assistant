package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "application_reviews")
public class ApplicationReview {
    @Id
    private String id;

    @Indexed(unique = true)
    private String applicationId;

    @Indexed
    private String reviewerId;

    private String reviewerRole;

    private String reviewStatus; // e.g. "UNDER_REVIEW", "APPROVED", "REJECTED", "CORRECTION_REQUIRED"

    private String remarks;

    private String decisionReason;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant completedAt;
}
