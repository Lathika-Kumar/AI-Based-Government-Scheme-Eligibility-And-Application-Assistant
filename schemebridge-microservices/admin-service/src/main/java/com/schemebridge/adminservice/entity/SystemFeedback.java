package com.schemebridge.adminservice.entity;

import com.schemebridge.adminservice.enums.FeedbackStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "SYSTEM_FEEDBACK", indexes = {
    @Index(name = "IDX_FB_ID", columnList = "FEEDBACK_ID", unique = true),
    @Index(name = "IDX_FB_STATUS", columnList = "STATUS"),
    @Index(name = "IDX_FB_USER", columnList = "USER_ID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FEEDBACK_ID", unique = true, nullable = false, length = 50)
    private String feedbackId;

    @Column(name = "USER_ID", nullable = false, length = 100)
    private String userId;

    @Column(name = "USER_EMAIL", length = 100)
    private String userEmail;

    @Column(name = "SUBJECT", nullable = false, length = 200)
    private String subject;

    @Column(name = "MESSAGE", nullable = false, length = 2000)
    private String message;

    @Column(name = "RATING")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.SUBMITTED;

    @Column(name = "ASSIGNED_OFFICER_ID", length = 50)
    private String assignedOfficerId;

    @Column(name = "RESOLUTION_NOTES", length = 1000)
    private String resolutionNotes;

    @Column(name = "RESOLVED_AT")
    private Instant resolvedAt;
}
