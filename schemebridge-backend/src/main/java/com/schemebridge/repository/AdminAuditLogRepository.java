package com.schemebridge.repository;

import com.schemebridge.entity.AdminAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends MongoRepository<AdminAuditLog, String> {
    List<AdminAuditLog> findByTargetIdOrderByCreatedAtDesc(String targetId);
    List<AdminAuditLog> findByActorEmailOrderByCreatedAtDesc(String actorEmail);
}
