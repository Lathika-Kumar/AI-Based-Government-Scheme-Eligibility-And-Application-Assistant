package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.SystemFeedback;
import com.schemebridge.adminservice.enums.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemFeedbackRepository extends JpaRepository<SystemFeedback, Long> {

    Optional<SystemFeedback> findByFeedbackId(String feedbackId);

    Page<SystemFeedback> findByActiveTrue(Pageable pageable);

    Page<SystemFeedback> findByStatusAndActiveTrue(FeedbackStatus status, Pageable pageable);

    Page<SystemFeedback> findBySubjectContainingIgnoreCaseOrMessageContainingIgnoreCaseAndActiveTrue(String query1, String query2, Pageable pageable);

    Long countByStatus(FeedbackStatus status);
}
