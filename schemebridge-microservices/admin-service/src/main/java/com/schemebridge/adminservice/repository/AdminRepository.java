package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.Admin;
import com.schemebridge.adminservice.enums.OfficerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdminId(String adminId);

    Optional<Admin> findByEmail(String email);

    Boolean existsByEmail(String email);

    Long countByStatus(OfficerStatus status);
}
