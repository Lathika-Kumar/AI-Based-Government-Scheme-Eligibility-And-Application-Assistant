package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, String> {

    Optional<NotificationPreference> findByAuthUserId(String authUserId);

    Boolean existsByAuthUserId(String authUserId);
}
