package com.schemebridge.repository;

import com.schemebridge.entity.Grievance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrievanceRepository extends MongoRepository<Grievance, String> {
    List<Grievance> findByUserId(String userId);
    Page<Grievance> findByUserId(String userId, Pageable pageable);
    List<Grievance> findAllByOrderByCreatedAtDesc();
}
