package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AdminAuditLogRepository extends MongoRepository<AdminAuditLog, String> {
    Page<AdminAuditLog> findAllByActorUserId(String actorUserId, Pageable pageable);
    Page<AdminAuditLog> findAllByEntityType(String entityType, Pageable pageable);
    Page<AdminAuditLog> findAllByAction(String action, Pageable pageable);
    Page<AdminAuditLog> findAllByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}
