package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.ApplicationEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationEventRepository extends MongoRepository<ApplicationEvent, String> {
    List<ApplicationEvent> findAllByApplicationIdOrderByCreatedAtAsc(String applicationId);
}
