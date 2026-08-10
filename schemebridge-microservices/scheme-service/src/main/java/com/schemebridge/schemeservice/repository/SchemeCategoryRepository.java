package com.schemebridge.schemeservice.repository;

import com.schemebridge.schemeservice.entity.SchemeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeCategoryRepository extends JpaRepository<SchemeCategory, String> {
    List<SchemeCategory> findByStatusOrderByDisplayOrderAsc(String status);
    Optional<SchemeCategory> findByCode(String code);
}
