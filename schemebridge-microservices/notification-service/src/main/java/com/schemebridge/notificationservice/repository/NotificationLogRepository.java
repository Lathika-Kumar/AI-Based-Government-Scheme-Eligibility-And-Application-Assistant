package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {

    List<NotificationLog> findByNotificationId(String notificationId);

    Page<NotificationLog> findByAuthUserId(String authUserId, Pageable pageable);

    void deleteByTimestampBefore(Instant cutoff);
}
