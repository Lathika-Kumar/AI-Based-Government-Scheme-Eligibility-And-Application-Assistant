package com.schemebridge.adminservice.entity;

import com.schemebridge.adminservice.enums.AdminActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ADMIN_ACTIVITY_LOG", indexes = {
    @Index(name = "IDX_ACTIVITY_ACTOR", columnList = "ACTOR_EMAIL"),
    @Index(name = "IDX_ACTIVITY_ACTION", columnList = "ACTION_TYPE"),
    @Index(name = "IDX_ACTIVITY_TIME", columnList = "TIMESTAMP")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LOG_ID", unique = true, nullable = false, length = 50)
    private String logId;

    @Column(name = "ACTOR_EMAIL", nullable = false, length = 100)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE", nullable = false, length = 50)
    private AdminActionType actionType;

    @Column(name = "TARGET_TYPE", length = 50)
    private String targetType;

    @Column(name = "TARGET_ID", length = 100)
    private String targetId;

    @Column(name = "DETAILS", length = 1000)
    private String details;

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Column(name = "TIMESTAMP", nullable = false)
    private Instant timestamp;
}
