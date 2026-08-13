package com.schemebridge.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Standard DomainEvent model exchanged across SchemeBridge Microservices
 * for business-event-driven asynchronous communication and notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;         // SCHEME_CREATED, APPLICATION_APPROVED, DOCUMENT_VERIFIED, etc.
    private String sourceModule;      // CORE_SCHEME, CORE_APPLICATION, CORE_DOCUMENT, ADMIN_ANNOUNCEMENT, ADMIN_GRIEVANCE

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String authUserId;             // Single target citizen authUserId
    private List<String> targetAuthUserIds; // Multi-citizen / bulk target list
    private String targetAudience;        // ALL_USERS, ELIGIBLE_CITIZENS, FARMERS, etc.

    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    private List<String> preferredChannels; // EMAIL, SMS, PUSH, IN_APP

    public DomainEvent addPayload(String key, Object value) {
        if (this.payload == null) {
            this.payload = new HashMap<>();
        }
        if (value != null) {
            this.payload.put(key, value);
        }
        return this;
    }
}
