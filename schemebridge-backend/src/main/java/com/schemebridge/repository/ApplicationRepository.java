package com.schemebridge.repository;

import com.schemebridge.entity.Application;
import com.schemebridge.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {

    List<Application> findByUserId(String userId);

    Page<Application> findByUserId(String userId, Pageable pageable);

    Page<Application> findByUserIdAndStatus(String userId, ApplicationStatus status, Pageable pageable);

    Optional<Application> findByUserIdAndSchemeId(String userId, String schemeId);

    Optional<Application> findByReferenceNumber(String referenceNumber);
}
