package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.SchemeVerifiedData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeVerifiedDataRepository extends MongoRepository<SchemeVerifiedData, String> {

    Optional<SchemeVerifiedData> findBySchemeCode(String schemeCode);

    List<SchemeVerifiedData> findBySchemeCodeIn(java.util.Collection<String> schemeCodes);

    Optional<SchemeVerifiedData> findBySlug(String slug);

    List<SchemeVerifiedData> findByDocumentStatus(String documentStatus);

    List<SchemeVerifiedData> findByReconciliationStatus(String reconciliationStatus);

    Page<SchemeVerifiedData> findAll(Pageable pageable);

    long countByDocumentStatus(String documentStatus);

    long countByReconciliationStatus(String reconciliationStatus);
}
