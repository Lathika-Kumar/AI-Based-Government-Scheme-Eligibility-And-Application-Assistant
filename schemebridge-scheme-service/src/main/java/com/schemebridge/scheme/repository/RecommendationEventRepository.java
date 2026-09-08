package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.RecommendationEvent;
import com.schemebridge.scheme.document.RecommendationEventType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationEventRepository extends MongoRepository<RecommendationEvent, String> {
    List<RecommendationEvent> findByUserId(String userId);
    List<RecommendationEvent> findBySchemeCode(String schemeCode);
    List<RecommendationEvent> findByEventType(RecommendationEventType eventType);
    long countByEventType(RecommendationEventType eventType);
    long countByUserId(String userId);
    long countBySchemeCode(String schemeCode);
    boolean existsBySessionIdAndSchemeCodeAndEventType(String sessionId, String schemeCode, RecommendationEventType eventType);
    java.util.Optional<RecommendationEvent> findFirstBySessionIdAndSchemeCodeAndEventType(String sessionId, String schemeCode, RecommendationEventType eventType);
}
