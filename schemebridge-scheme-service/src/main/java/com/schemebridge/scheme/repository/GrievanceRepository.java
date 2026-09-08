package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.Grievance;
import com.schemebridge.scheme.document.GrievanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrievanceRepository extends MongoRepository<Grievance, String> {
    Optional<Grievance> findByGrievanceNumber(String grievanceNumber);
    List<Grievance> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Page<Grievance> findAllByUserId(String userId, Pageable pageable);
    Page<Grievance> findAllByStatus(GrievanceStatus status, Pageable pageable);
    Page<Grievance> findAllByAssignedTo(String assignedTo, Pageable pageable);
    long countByStatus(GrievanceStatus status);
    long countByStatusInAndCreatedAtBefore(List<GrievanceStatus> statuses, Instant cutoff);
}
