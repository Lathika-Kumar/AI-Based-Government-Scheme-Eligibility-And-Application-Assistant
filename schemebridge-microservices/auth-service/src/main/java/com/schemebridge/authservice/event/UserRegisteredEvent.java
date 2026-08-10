package com.schemebridge.authservice.event;

import java.time.LocalDateTime;

public class UserRegisteredEvent {
    private final String userId;
    private final String email;
    private final LocalDateTime timestamp;

    public UserRegisteredEvent(String userId, String email) {
        this.userId = userId;
        this.email = email;
        this.timestamp = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
