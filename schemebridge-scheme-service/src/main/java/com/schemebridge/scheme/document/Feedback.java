package com.schemebridge.scheme.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "portal_feedback")
public class Feedback {

    @Id
    private String id;

    @Indexed(unique = true)
    private String feedbackNumber;

    @Indexed
    private String userId;

    private String citizenEmail;

    private String citizenName;

    private String type;

    private Integer rating;

    private String comment;

    private String relatedScheme;

    @Builder.Default
    private String status = "RECEIVED";

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
