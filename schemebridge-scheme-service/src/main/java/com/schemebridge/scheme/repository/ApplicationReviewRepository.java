package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.ApplicationReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationReviewRepository extends MongoRepository<ApplicationReview, String> {
    Optional<ApplicationReview> findByApplicationId(String applicationId);
}
