package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.Officer;
import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    Optional<Officer> findByOfficerId(String officerId);

    Optional<Officer> findByEmail(String email);

    Boolean existsByOfficerId(String officerId);

    Boolean existsByEmail(String email);

    Page<Officer> findByActiveTrue(Pageable pageable);

    Page<Officer> findByRoleAndActiveTrue(OfficerRole role, Pageable pageable);

    Page<Officer> findByStatusAndActiveTrue(OfficerStatus status, Pageable pageable);

    Page<Officer> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndActiveTrue(String nameQuery, String emailQuery, Pageable pageable);

    Long countByStatus(OfficerStatus status);

    Long countByRole(OfficerRole role);
}
