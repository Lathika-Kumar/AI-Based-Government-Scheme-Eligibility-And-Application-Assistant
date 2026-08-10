package com.schemebridge.notificationservice.scheduler;

import com.schemebridge.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "${schemebridge.notification.scheduler.retry-cron:0 */2 * * * *}")
    public void runRetryAndScheduledJobs() {
        log.debug("Running background retry and scheduled notification tasks...");
        try {
            notificationService.retryFailedNotifications();
            notificationService.executeScheduledNotifications();
        } catch (Exception ex) {
            log.error("Error executing background notification scheduler task", ex);
        }
    }
}
