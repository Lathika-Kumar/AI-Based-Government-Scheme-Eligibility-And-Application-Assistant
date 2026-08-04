package com.schemebridge.repository;

import com.schemebridge.entity.SchemeAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeAuditLogRepository extends MongoRepository<SchemeAuditLog, String> {
    List<SchemeAuditLog> findBySchemeIdOrderByUpdatedAtDesc(String schemeId);
}
