package com.schemebridge.adminservice.scheduler;

import com.schemebridge.adminservice.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminScheduler {

    private final AnnouncementService announcementService;

    @Scheduled(cron = "${schemebridge.admin.scheduler.announcement-cron:0 0 * * * *}")
    public void runAnnouncementCleanupJob() {
        log.debug("Running background announcement cleanup job...");
        try {
            announcementService.cleanupExpiredAnnouncements();
        } catch (Exception ex) {
            log.error("Error running announcement cleanup job", ex);
        }
    }
}
