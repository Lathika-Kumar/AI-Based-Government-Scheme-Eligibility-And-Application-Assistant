package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String recipientUserId;
    private String recipientRole;
    private NotificationType type;
    private String title;
    private String message;
    private String channel;
    private String relatedEntityType;
    private String relatedEntityId;
    private boolean read;
    private Instant readAt;
    private String createdBy;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
