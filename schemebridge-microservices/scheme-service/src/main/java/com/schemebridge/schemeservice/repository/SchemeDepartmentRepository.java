package com.schemebridge.schemeservice.repository;

import com.schemebridge.schemeservice.entity.SchemeDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeDepartmentRepository extends JpaRepository<SchemeDepartment, String> {
    List<SchemeDepartment> findByStatus(String status);
    Optional<SchemeDepartment> findByCode(String code);
}
