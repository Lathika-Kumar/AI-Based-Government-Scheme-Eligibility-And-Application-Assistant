package com.schemebridge.notificationservice.repository;

import com.schemebridge.notificationservice.entity.NotificationStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface NotificationStatisticsRepository extends MongoRepository<NotificationStatistics, String> {

    Optional<NotificationStatistics> findByDate(LocalDate date);
}
