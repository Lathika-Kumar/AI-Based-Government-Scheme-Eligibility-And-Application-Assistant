package com.schemebridge.adminservice.service;

import com.schemebridge.adminservice.dto.AnnouncementRequest;
import com.schemebridge.adminservice.dto.AnnouncementResponse;
import com.schemebridge.adminservice.enums.AnnouncementAudience;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AnnouncementService {

    AnnouncementResponse createAnnouncement(String actorEmail, AnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(String actorEmail, String announcementId, AnnouncementRequest request);

    AnnouncementResponse getAnnouncementById(String announcementId);

    Page<AnnouncementResponse> getAllAnnouncements(int page, int size);

    List<AnnouncementResponse> getAnnouncementsForAudience(AnnouncementAudience audience);

    void broadcastAnnouncement(String actorEmail, String announcementId);

    void deleteAnnouncement(String actorEmail, String announcementId);

    void cleanupExpiredAnnouncements();
}
