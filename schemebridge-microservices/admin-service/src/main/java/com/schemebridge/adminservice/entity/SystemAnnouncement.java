package com.schemebridge.adminservice.entity;

import com.schemebridge.adminservice.enums.AnnouncementAudience;
import com.schemebridge.adminservice.enums.AnnouncementPriority;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "SYSTEM_ANNOUNCEMENTS", indexes = {
    @Index(name = "IDX_ANN_ID", columnList = "ANNOUNCEMENT_ID", unique = true),
    @Index(name = "IDX_ANN_PRIORITY", columnList = "PRIORITY"),
    @Index(name = "IDX_ANN_AUDIENCE", columnList = "TARGET_AUDIENCE")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAnnouncement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ANNOUNCEMENT_ID", unique = true, nullable = false, length = 50)
    private String announcementId;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRIORITY", nullable = false, length = 20)
    @Builder.Default
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_AUDIENCE", nullable = false, length = 30)
    @Builder.Default
    private AnnouncementAudience targetAudience = AnnouncementAudience.ALL_USERS;

    @Column(name = "SCHEDULED_AT")
    private Instant scheduledAt;

    @Column(name = "EXPIRES_AT")
    private Instant expiresAt;

    @Column(name = "BROADCASTED", nullable = false)
    @Builder.Default
    private Boolean broadcasted = false;
}
