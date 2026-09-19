package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.CitizenVaultDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenVaultDocumentRepository extends MongoRepository<CitizenVaultDocument, String> {

    List<CitizenVaultDocument> findByUserIdOrderByUploadedAtDesc(String userId);

    List<CitizenVaultDocument> findAllByUserId(String userId);

    Optional<CitizenVaultDocument> findByUserIdAndDocumentCode(String userId, String documentCode);

    Optional<CitizenVaultDocument> findFirstByUserIdAndDocumentTypeIgnoreCase(String userId, String documentType);

    Optional<CitizenVaultDocument> findFirstByUserIdAndCanonicalDocumentCodeIgnoreCase(String userId, String canonicalCode);

    Optional<CitizenVaultDocument> findFirstByUserIdAndDocumentNameIgnoreCase(String userId, String documentName);

    void deleteByUserIdAndId(String userId, String id);

    void deleteAllByUserId(String userId);
}
