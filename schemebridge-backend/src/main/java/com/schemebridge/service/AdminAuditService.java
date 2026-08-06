package com.schemebridge.service;

import com.schemebridge.entity.AdminAuditLog;

import java.util.List;

public interface AdminAuditService {
    List<AdminAuditLog> getAuditLogsForTarget(String targetId);
}
