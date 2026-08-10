package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.ScheduledNotification;
import com.schemebridge.notificationservice.enums.DeliveryStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScheduledNotificationRepository extends MongoRepository<ScheduledNotification, String> {

    List<ScheduledNotification> findByStatusAndScheduledTimeBeforeAndActiveTrue(DeliveryStatus status, Instant now);
}
