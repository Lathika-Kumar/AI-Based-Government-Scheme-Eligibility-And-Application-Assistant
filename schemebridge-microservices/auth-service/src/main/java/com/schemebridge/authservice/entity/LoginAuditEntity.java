package com.schemebridge.authservice.entity;

import com.schemebridge.authservice.enums.LoginStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LOGIN_AUDIT")
public class LoginAuditEntity {

    @Id
    @Column(name = "AUDIT_ID", length = 36, nullable = false)
    private String auditId;

    @Column(name = "USER_ID", length = 36)
    private String userId;

    @Column(name = "EMAIL", length = 100, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOGIN_STATUS", length = 50, nullable = false)
    private LoginStatus loginStatus;

    @Column(name = "CLIENT_IP", length = 45)
    private String clientIp;

    @Column(name = "USER_AGENT", length = 255)
    private String userAgent;

    @Column(name = "DEVICE_INFO", length = 100)
    private String deviceInfo;

    @Column(name = "FAILURE_REASON", length = 255)
    private String failureReason;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "CREATED_BY", length = 100, nullable = false)
    private String createdBy = "SYSTEM";

    public LoginAuditEntity() {}

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LoginStatus getLoginStatus() { return loginStatus; }
    public void setLoginStatus(LoginStatus loginStatus) { this.loginStatus = loginStatus; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
