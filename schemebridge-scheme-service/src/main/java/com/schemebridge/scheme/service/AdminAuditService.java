package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.AdminAuditLog;
import com.schemebridge.scheme.dto.response.AdminAuditLogResponse;
import com.schemebridge.scheme.dto.response.PagedAdminAuditLogResponse;
import com.schemebridge.scheme.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final MongoTemplate mongoTemplate;

    public AdminAuditLog recordAction(String actorUserId, String actorRole, String action,
                                     String entityType, String entityId,
                                     Object beforeState, Object afterState,
                                     String ipAddress, String userAgent,
                                     Map<String, Object> metadata) {
        AdminAuditLog logEntry = AdminAuditLog.builder()
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeState(beforeState)
                .afterState(afterState)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata != null ? metadata : Map.of())
                .createdAt(Instant.now())
                .build();

        AdminAuditLog saved = adminAuditLogRepository.save(logEntry);
        log.info("Recorded admin audit log: action={}, actor={}, entity={}:{}", action, actorUserId, entityType, entityId);
        return saved;
    }

    public PagedAdminAuditLogResponse getAuditLogs(String actor, String action, String entity,
                                                  Instant startDate, Instant endDate,
                                                  int page, int size, String sortField, String sortDir) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (actor != null && !actor.isBlank() && !"all".equalsIgnoreCase(actor)) {
            criteriaList.add(Criteria.where("actorUserId").regex(actor.trim(), "i"));
        }
        if (action != null && !action.isBlank() && !"all".equalsIgnoreCase(action)) {
            criteriaList.add(Criteria.where("action").is(action.trim()));
        }
        if (entity != null && !entity.isBlank() && !"all".equalsIgnoreCase(entity)) {
            criteriaList.add(Criteria.where("entityType").is(entity.trim()));
        }
        if (startDate != null || endDate != null) {
            Criteria dateCrit = Criteria.where("createdAt");
            if (startDate != null) dateCrit = dateCrit.gte(startDate);
            if (endDate != null) dateCrit = dateCrit.lte(endDate);
            criteriaList.add(dateCrit);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalElements = mongoTemplate.count(query, AdminAuditLog.class);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"), sortField != null ? sortField : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        query.with(pageable);

        List<AdminAuditLog> logs = mongoTemplate.find(query, AdminAuditLog.class);
        List<AdminAuditLogResponse> content = logs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return PagedAdminAuditLogResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog entry) {
        return AdminAuditLogResponse.builder()
                .id(entry.getId())
                .actorUserId(entry.getActorUserId())
                .actorRole(entry.getActorRole())
                .action(entry.getAction())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .beforeState(entry.getBeforeState())
                .afterState(entry.getAfterState())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .metadata(entry.getMetadata())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
