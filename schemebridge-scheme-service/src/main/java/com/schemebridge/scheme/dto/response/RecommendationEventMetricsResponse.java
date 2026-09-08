package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEventMetricsResponse {
    private long totalEvents;
    private Map<String, Long> eventsByType;
    private long uniqueCitizens;
    private long uniqueSchemes;
    private String currentModelVersion;
    private Instant calculatedAt;
}
