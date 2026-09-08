package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Feedback> findByFeedbackNumber(String feedbackNumber);

    long countByStatus(String status);
}
