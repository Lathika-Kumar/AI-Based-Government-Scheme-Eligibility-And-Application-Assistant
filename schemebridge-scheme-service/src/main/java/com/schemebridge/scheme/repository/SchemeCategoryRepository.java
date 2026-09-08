package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.SchemeCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface SchemeCategoryRepository extends MongoRepository<SchemeCategory, String> {
    Optional<SchemeCategory> findByCode(String code);
    List<SchemeCategory> findAllByStatus(String status);
}
