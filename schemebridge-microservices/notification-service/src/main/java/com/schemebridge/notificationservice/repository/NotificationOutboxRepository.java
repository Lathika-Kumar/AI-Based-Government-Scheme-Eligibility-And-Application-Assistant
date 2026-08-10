package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationOutbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends MongoRepository<NotificationOutbox, String> {

    List<NotificationOutbox> findByProcessedFalse();
}
