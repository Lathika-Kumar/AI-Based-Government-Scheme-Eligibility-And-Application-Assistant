package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.AdminActivityLogResponse;
import com.schemebridge.adminservice.enums.AdminActionType;
import org.springframework.data.domain.Page;

public interface AuditLogService {

    void logActivity(String actorEmail, AdminActionType actionType, String targetType, String targetId, String details);

    Page<AdminActivityLogResponse> getAllLogs(int page, int size);

    Page<AdminActivityLogResponse> getLogsByActor(String actorEmail, int page, int size);

    Page<AdminActivityLogResponse> getLogsByAction(AdminActionType actionType, int page, int size);

    Page<AdminActivityLogResponse> getRecentLogs(int page, int size);
}
