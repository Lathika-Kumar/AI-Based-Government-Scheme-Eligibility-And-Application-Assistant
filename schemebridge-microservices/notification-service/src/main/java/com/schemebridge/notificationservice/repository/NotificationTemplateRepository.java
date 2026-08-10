package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationTemplate;
import com.schemebridge.notificationservice.enums.Channel;
import com.schemebridge.notificationservice.enums.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByTemplateId(String templateId);

    Optional<NotificationTemplate> findByNotificationTypeAndChannelAndActiveTrue(NotificationType notificationType, Channel channel);

    Boolean existsByTemplateId(String templateId);
}
