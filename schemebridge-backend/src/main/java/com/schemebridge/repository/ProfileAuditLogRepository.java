package com.schemebridge.repository;

import com.schemebridge.entity.ProfileAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileAuditLogRepository extends MongoRepository<ProfileAuditLog, String> {
    List<ProfileAuditLog> findByUserIdOrderByUpdatedAtDesc(String userId);
}
