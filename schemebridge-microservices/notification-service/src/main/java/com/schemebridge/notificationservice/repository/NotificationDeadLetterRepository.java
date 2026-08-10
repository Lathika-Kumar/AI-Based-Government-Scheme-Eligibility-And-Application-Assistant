package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationDeadLetter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDeadLetterRepository extends MongoRepository<NotificationDeadLetter, String> {

    List<NotificationDeadLetter> findByReplayedFalse();
}
