package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findAllByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId, Pageable pageable);
    Page<Notification> findAllByRecipientRoleOrderByCreatedAtDesc(String recipientRole, Pageable pageable);
    long countByRecipientUserIdAndReadFalse(String recipientUserId);
    long countByRecipientRoleAndReadFalse(String recipientRole);
    List<Notification> findAllByRecipientUserIdAndReadFalse(String recipientUserId);
}
