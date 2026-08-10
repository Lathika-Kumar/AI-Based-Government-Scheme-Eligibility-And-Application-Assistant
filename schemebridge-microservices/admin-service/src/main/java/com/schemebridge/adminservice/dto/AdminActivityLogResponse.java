package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.AdminActionType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityLogResponse {

    private Long id;
    private String logId;
    private String actorEmail;
    private AdminActionType actionType;
    private String targetType;
    private String targetId;
    private String details;
    private String ipAddress;
    private Instant timestamp;
}
