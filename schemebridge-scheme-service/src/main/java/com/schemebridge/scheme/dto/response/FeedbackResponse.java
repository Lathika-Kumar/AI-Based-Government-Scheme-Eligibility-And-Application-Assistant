package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private String id;
    private String feedbackNumber;
    private String userId;
    private String citizenEmail;
    private String citizenName;
    private String type;
    private Integer rating;
    private String comment;
    private String relatedScheme;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public static FeedbackResponse fromEntity(Feedback feedback) {
        if (feedback == null) return null;
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .feedbackNumber(feedback.getFeedbackNumber())
                .userId(feedback.getUserId())
                .citizenEmail(feedback.getCitizenEmail())
                .citizenName(feedback.getCitizenName())
                .type(feedback.getType())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .relatedScheme(feedback.getRelatedScheme())
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
