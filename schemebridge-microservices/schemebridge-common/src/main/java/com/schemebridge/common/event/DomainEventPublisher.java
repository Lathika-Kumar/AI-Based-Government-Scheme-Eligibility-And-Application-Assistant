package com.schemebridge.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Non-blocking Publisher component for emitting DomainEvent instances to Notification Service.
 */
@Component
@Slf4j
public class DomainEventPublisher {

    @Value("${integration.notification-service.url:http://localhost:8086}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void publishEvent(DomainEvent event) {
        if (event == null) return;
        log.info("[DomainEventPublisher] Emitting Event: ID={}, Type={}, Source={}, TargetUser={}",
                event.getEventId(), event.getEventType(), event.getSourceModule(), event.getAuthUserId());

        executorService.submit(() -> {
            try {
                String targetUrl = notificationServiceUrl + "/api/v1/notifications/events";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<DomainEvent> request = new HttpEntity<>(event, headers);

                restTemplate.postForObject(targetUrl, request, String.class);
                log.info("[DomainEventPublisher] Event ID {} successfully delivered to Notification Service at {}", event.getEventId(), targetUrl);
            } catch (Exception e) {
                log.warn("[DomainEventPublisher] Event ID {} delivery failed to {}: {}", event.getEventId(), notificationServiceUrl, e.getMessage());
            }
        });
    }
}
