package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.ApplicationDocument;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationDocumentRepository extends MongoRepository<ApplicationDocument, String> {
    List<ApplicationDocument> findAllByApplicationId(String applicationId);
    Optional<ApplicationDocument> findByApplicationIdAndDocumentCode(String applicationId, String documentCode);
    long countByVerificationStatus(DocumentVerificationStatus status);
    long countByUploadedTrue();
    Page<ApplicationDocument> findAllByVerificationStatus(DocumentVerificationStatus status, Pageable pageable);
    Page<ApplicationDocument> findAllByUploadedTrue(Pageable pageable);
}

