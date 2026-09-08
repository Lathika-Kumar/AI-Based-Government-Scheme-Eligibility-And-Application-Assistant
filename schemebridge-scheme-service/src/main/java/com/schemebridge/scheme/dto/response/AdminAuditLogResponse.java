package com.schemebridge.scheme.dto.response;

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
public class AdminAuditLogResponse {
    private String id;
    private String actorUserId;
    private String actorRole;
    private String action;
    private String entityType;
    private String entityId;
    private Object beforeState;
    private Object afterState;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
