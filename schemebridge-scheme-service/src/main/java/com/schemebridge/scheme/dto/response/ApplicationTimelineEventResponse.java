package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationTimelineEventResponse {
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private String message;
    private Instant createdAt;
    private Map<String, Object> metadata;
}
