package com.schemebridge.authservice.entity;

import com.schemebridge.authservice.enums.SecurityEventType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SECURITY_EVENTS")
public class SecurityEventEntity {

    @Id
    @Column(name = "EVENT_ID", length = 36, nullable = false)
    private String eventId;

    @Column(name = "USER_ID", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", length = 50, nullable = false)
    private SecurityEventType eventType;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "DETAILS", length = 512)
    private String details;

    @Column(name = "STATUS", length = 30, nullable = false)
    private String status = "PROCESSED";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "CREATED_BY", length = 100, nullable = false)
    private String createdBy = "SYSTEM";

    public SecurityEventEntity() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public SecurityEventType getEventType() { return eventType; }
    public void setEventType(SecurityEventType eventType) { this.eventType = eventType; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
