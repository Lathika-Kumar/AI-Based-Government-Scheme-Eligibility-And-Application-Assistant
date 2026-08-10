package com.schemebridge.authservice.repository;

import com.schemebridge.authservice.entity.SecurityEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEventEntity, String> {
    List<SecurityEventEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
