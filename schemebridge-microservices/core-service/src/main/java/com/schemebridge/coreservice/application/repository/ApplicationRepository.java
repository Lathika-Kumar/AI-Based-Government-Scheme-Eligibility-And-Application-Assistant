package com.schemebridge.coreservice.application.repository;

import com.schemebridge.coreservice.application.model.ApplicationDocument;
import com.schemebridge.coreservice.application.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<ApplicationDocument, String> {

    Optional<ApplicationDocument> findByApplicationNumber(String applicationNumber);

    List<ApplicationDocument> findByAuthUserIdOrderByCreatedAtDesc(String authUserId);

    Page<ApplicationDocument> findByAuthUserIdOrderByCreatedAtDesc(String authUserId, Pageable pageable);

    List<ApplicationDocument> findByAuthUserIdAndApplicationStatusOrderByCreatedAtDesc(
            String authUserId, ApplicationStatus status);

    // Check for duplicate active application
    @Query("{ 'authUserId': ?0, 'schemeId': ?1, 'applicationStatus': { $in: ?2 } }")
    List<ApplicationDocument> findActiveApplicationsByUserAndScheme(
            String authUserId, String schemeId, List<ApplicationStatus> activeStatuses);

    long countByAuthUserId(String authUserId);
    long countByAuthUserIdAndApplicationStatus(String authUserId, ApplicationStatus status);

    Page<ApplicationDocument> findByDepartmentIdAndApplicationStatusOrderByCreatedAtDesc(
            String departmentId, ApplicationStatus status, Pageable pageable);

    Page<ApplicationDocument> findByApplicationStatusOrderByCreatedAtDesc(
            ApplicationStatus status, Pageable pageable);

    Page<ApplicationDocument> findBySchemeIdOrderByCreatedAtDesc(String schemeId, Pageable pageable);

    // Search across multiple fields
    @Query("{ $and: [ { 'authUserId': ?0 }, { $or: [ " +
           "{ 'applicationNumber': { $regex: ?1, $options: 'i' } }, " +
           "{ 'schemeName': { $regex: ?1, $options: 'i' } }, " +
           "{ 'departmentName': { $regex: ?1, $options: 'i' } } " +
           "] } ] }")
    Page<ApplicationDocument> searchByUser(String authUserId, String keyword, Pageable pageable);

    boolean existsByAuthUserIdAndSchemeIdAndApplicationStatusIn(
            String authUserId, String schemeId, List<ApplicationStatus> statuses);
}
