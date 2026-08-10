package com.schemebridge.adminservice.factory;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.entity.AdminActivityLog;
import com.schemebridge.adminservice.enums.AdminActionType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory Pattern implementation for constructing AdminActivityLog audit entries.
 */
@Component
public class AdminAuditFactory {

    public AdminActivityLog createActivityLog(String actorEmail, AdminActionType actionType, String targetType, String targetId, String details, String ipAddress) {
        return AdminActivityLog.builder()
                .logId(AdminConstants.AUDIT_ID_PREFIX + UUID.randomUUID().toString().substring(0, 13).toUpperCase())
                .actorEmail(actorEmail != null ? actorEmail : AdminConstants.SYSTEM_ACTOR)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .timestamp(Instant.now())
                .build();
    }
}
