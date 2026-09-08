package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.DocumentOcrResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentOcrResultRepository extends MongoRepository<DocumentOcrResult, String> {
    Optional<DocumentOcrResult> findByApplicationIdAndDocumentCodeAndVersion(String applicationId, String documentCode, Integer version);
    List<DocumentOcrResult> findAllByApplicationId(String applicationId);
    List<DocumentOcrResult> findAllByUserId(String userId);
    void deleteAllByApplicationId(String applicationId);
}
