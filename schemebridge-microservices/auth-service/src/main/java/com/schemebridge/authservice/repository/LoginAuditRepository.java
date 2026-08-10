package com.schemebridge.authservice.repository;

import com.schemebridge.authservice.entity.LoginAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAuditEntity, String> {
    List<LoginAuditEntity> findByEmailOrderByCreatedAtDesc(String email);
}
