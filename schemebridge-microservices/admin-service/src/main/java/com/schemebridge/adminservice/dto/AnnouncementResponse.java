package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.AnnouncementAudience;
import com.schemebridge.adminservice.enums.AnnouncementPriority;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementResponse {

    private Long id;
    private String announcementId;
    private String title;
    private String content;
    private AnnouncementPriority priority;
    private AnnouncementAudience targetAudience;
    private Instant scheduledAt;
    private Instant expiresAt;
    private Boolean broadcasted;
    private Instant createdAt;
    private Instant updatedAt;
}
