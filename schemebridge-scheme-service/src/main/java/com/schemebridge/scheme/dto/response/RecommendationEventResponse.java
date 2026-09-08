package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.RecommendationContextSnapshot;
import com.schemebridge.scheme.document.RecommendationEventType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEventResponse {
    private String eventId;
    private String userId;
    private String schemeCode;
    private RecommendationEventType eventType;
    private RecommendationContextSnapshot recommendationContext;
    private Instant timestamp;
    private String status;
}
