package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.AdminActivityLog;
import com.schemebridge.adminservice.enums.AdminActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {

    Optional<AdminActivityLog> findByLogId(String logId);

    Page<AdminActivityLog> findByActorEmail(String actorEmail, Pageable pageable);

    Page<AdminActivityLog> findByActionType(AdminActionType actionType, Pageable pageable);

    Page<AdminActivityLog> findByActiveTrue(Pageable pageable);
}
