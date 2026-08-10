package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.constants.AdminConstants;
import com.schemebridge.adminservice.dto.AdminActivityLogResponse;
import com.schemebridge.adminservice.entity.AdminActivityLog;
import com.schemebridge.adminservice.enums.AdminActionType;
import com.schemebridge.adminservice.factory.AdminAuditFactory;
import com.schemebridge.adminservice.repository.AdminActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AdminActivityLogRepository auditRepository;
    private final AdminAuditFactory auditFactory;

    @Override
    public void logActivity(String actorEmail, AdminActionType actionType, String targetType, String targetId, String details) {
        log.info("[AUDIT LOG] Actor: {}, Action: {}, Target: {}:{}, Details: {}", actorEmail, actionType, targetType, targetId, details);
        AdminActivityLog logEntity = auditFactory.createActivityLog(actorEmail, actionType, targetType, targetId, details, "127.0.0.1");
        auditRepository.save(logEntity);
    }

    @Override
    public Page<AdminActivityLogResponse> getAllLogs(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditRepository.findByActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<AdminActivityLogResponse> getLogsByActor(String actorEmail, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditRepository.findByActorEmail(actorEmail, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<AdminActivityLogResponse> getLogsByAction(AdminActionType actionType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditRepository.findByActionType(actionType, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<AdminActivityLogResponse> getRecentLogs(int page, int size) {
        return getAllLogs(page, size);
    }

    private AdminActivityLogResponse mapToResponse(AdminActivityLog l) {
        return AdminActivityLogResponse.builder()
                .id(l.getId())
                .logId(l.getLogId())
                .actorEmail(l.getActorEmail())
                .actionType(l.getActionType())
                .targetType(l.getTargetType())
                .targetId(l.getTargetId())
                .details(l.getDetails())
                .ipAddress(l.getIpAddress())
                .timestamp(l.getTimestamp())
                .build();
    }
}
