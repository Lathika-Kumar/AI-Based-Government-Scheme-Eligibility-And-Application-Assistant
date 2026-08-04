package com.schemebridge.repository;

import com.schemebridge.entity.Document;
import com.schemebridge.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    List<Document> findByUserId(String userId);

    Page<Document> findByUserId(String userId, Pageable pageable);

    Page<Document> findByUserIdAndStatus(String userId, DocumentStatus status, Pageable pageable);
}
