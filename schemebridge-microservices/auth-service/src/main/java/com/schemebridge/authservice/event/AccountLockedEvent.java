package com.schemebridge.authservice.event;

import java.time.LocalDateTime;

public class AccountLockedEvent {
    private final String userId;
    private final String email;
    private final String reason;
    private final LocalDateTime timestamp;

    public AccountLockedEvent(String userId, String email, String reason) {
        this.userId = userId;
        this.email = email;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
