package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.DocumentVerificationResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVerificationResultRepository extends MongoRepository<DocumentVerificationResult, String> {

    Optional<DocumentVerificationResult> findByApplicationIdAndDocumentCodeAndVersion(
            String applicationId, String documentCode, Integer version);

    Optional<DocumentVerificationResult> findTopByApplicationIdAndDocumentCodeOrderByVersionDesc(
            String applicationId, String documentCode);

    Optional<DocumentVerificationResult> findTopByDocumentIdOrderByVersionDesc(String documentId);

    List<DocumentVerificationResult> findAllByApplicationId(String applicationId);

    Optional<DocumentVerificationResult> findFirstBySha256AndDocumentIdNot(String sha256, String documentId);
}
