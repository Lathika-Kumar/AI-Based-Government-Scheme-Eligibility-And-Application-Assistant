package com.schemebridge.authservice.entity;

import com.schemebridge.authservice.enums.OtpType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OTP_VERIFICATIONS")
public class OtpVerificationEntity {

    @Id
    @Column(name = "OTP_ID", length = 36, nullable = false)
    private String otpId;

    @Column(name = "EMAIL", length = 100, nullable = false)
    private String email;

    @Column(name = "OTP_HASH", length = 255, nullable = false)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "OTP_TYPE", length = 30, nullable = false)
    private OtpType otpType;

    @Column(name = "EXPIRY_TIME", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "VERIFIED", nullable = false)
    private boolean verified = false;

    @Column(name = "ATTEMPTS", nullable = false)
    private int attempts = 0;

    @Column(name = "STATUS", length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "CREATED_BY", length = 100, nullable = false)
    private String createdBy = "SYSTEM";

    @Column(name = "UPDATED_BY", length = 100, nullable = false)
    private String updatedBy = "SYSTEM";

    public OtpVerificationEntity() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getOtpId() { return otpId; }
    public void setOtpId(String otpId) { this.otpId = otpId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public OtpType getOtpType() { return otpType; }
    public void setOtpType(OtpType otpType) { this.otpType = otpType; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
