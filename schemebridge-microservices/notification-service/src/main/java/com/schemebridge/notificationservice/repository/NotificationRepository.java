package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.Notification;
import com.schemebridge.notificationservice.enums.DeliveryStatus;
import com.schemebridge.notificationservice.enums.ReadStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Optional<Notification> findByNotificationId(String notificationId);

    Page<Notification> findByAuthUserIdAndActiveTrue(String authUserId, Pageable pageable);

    Page<Notification> findByAuthUserIdAndReadStatusAndActiveTrue(String authUserId, ReadStatus readStatus, Pageable pageable);

    List<Notification> findByAuthUserIdAndReadStatusAndActiveTrue(String authUserId, ReadStatus readStatus);

    Long countByAuthUserIdAndReadStatusAndActiveTrue(String authUserId, ReadStatus readStatus);

    List<Notification> findByDeliveryStatusAndRetryCountLessThanAndActiveTrue(DeliveryStatus deliveryStatus, Integer maxRetryCount);

    Page<Notification> findByAuthUserIdAndSubjectContainingIgnoreCaseOrMessageContainingIgnoreCaseAndActiveTrue(
            String authUserId, String subjectQuery, String messageQuery, Pageable pageable);

    List<Notification> findByExpiresAtBeforeAndActiveTrue(Instant now);
}
