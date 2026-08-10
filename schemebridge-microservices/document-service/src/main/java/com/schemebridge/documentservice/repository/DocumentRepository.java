package com.schemebridge.documentservice.repository;

import com.schemebridge.documentservice.document.DocumentMetadata;
import com.schemebridge.documentservice.enums.DocumentType;
import com.schemebridge.documentservice.enums.VerificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentMetadata, String> {

    List<DocumentMetadata> findByAuthUserIdAndActiveTrue(String authUserId);

    Optional<DocumentMetadata> findByIdAndActiveTrue(String id);

    List<DocumentMetadata> findByApplicationIdAndActiveTrue(String applicationId);

    Optional<DocumentMetadata> findByAuthUserIdAndDocumentTypeAndActiveTrue(String authUserId, DocumentType documentType);

    Optional<DocumentMetadata> findByAuthUserIdAndDocumentTypeAndChecksumSHA256AndActiveTrue(
            String authUserId, DocumentType documentType, String checksumSHA256);

    List<DocumentMetadata> findByAuthUserIdAndVerificationStatusAndActiveTrue(
            String authUserId, VerificationStatus status);

    long countByAuthUserIdAndActiveTrue(String authUserId);

    long countByAuthUserIdAndVerificationStatusAndActiveTrue(String authUserId, VerificationStatus status);
}
