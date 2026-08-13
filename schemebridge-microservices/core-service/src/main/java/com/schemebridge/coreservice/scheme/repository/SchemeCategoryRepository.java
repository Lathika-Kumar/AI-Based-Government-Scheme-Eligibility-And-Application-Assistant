package com.schemebridge.coreservice.scheme.repository;

import com.schemebridge.coreservice.scheme.entity.SchemeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeCategoryRepository extends JpaRepository<SchemeCategory, String> {
    List<SchemeCategory> findByStatusOrderByDisplayOrderAsc(String status);
    Optional<SchemeCategory> findByCode(String code);
}
