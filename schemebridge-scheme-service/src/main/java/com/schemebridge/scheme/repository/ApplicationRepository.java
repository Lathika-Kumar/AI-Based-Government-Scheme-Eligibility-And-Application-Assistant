package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.Application;
import com.schemebridge.scheme.document.ApplicationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {
    Optional<Application> findByApplicationNumber(String applicationNumber);
    List<Application> findAllByUserId(String userId);
    Optional<Application> findByUserIdAndSchemeCode(String userId, String schemeCode);
    Optional<Application> findFirstByUserIdAndSchemeCodeOrderByCreatedAtDesc(String userId, String schemeCode);
    Optional<Application> findByUserIdAndSchemeCodeAndStatusIn(String userId, String schemeCode, List<ApplicationStatus> statuses);
    List<Application> findAllByUserIdAndSchemeCodeAndStatusIn(String userId, String schemeCode, List<ApplicationStatus> statuses);
    Optional<Application> findFirstByOrderByApplicationNumberDesc();
    long countByStatus(ApplicationStatus status);
}

