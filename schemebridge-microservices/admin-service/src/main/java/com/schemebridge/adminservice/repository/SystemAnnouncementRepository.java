package com.schemebridge.adminservice.repository;

import com.schemebridge.adminservice.entity.SystemAnnouncement;
import com.schemebridge.adminservice.enums.AnnouncementAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, Long> {

    Optional<SystemAnnouncement> findByAnnouncementId(String announcementId);

    Page<SystemAnnouncement> findByActiveTrue(Pageable pageable);

    List<SystemAnnouncement> findByTargetAudienceAndActiveTrue(AnnouncementAudience audience);

    List<SystemAnnouncement> findByExpiresAtBeforeAndActiveTrue(Instant now);
}
