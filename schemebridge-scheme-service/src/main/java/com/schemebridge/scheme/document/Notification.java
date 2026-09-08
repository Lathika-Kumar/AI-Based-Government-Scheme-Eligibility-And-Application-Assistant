package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
@CompoundIndexes({
    @CompoundIndex(name = "user_created_idx", def = "{'recipientUserId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "user_read_idx", def = "{'recipientUserId': 1, 'read': 1}")
})
public class Notification {

    @Id
    private String id;

    @Indexed
    private String recipientUserId;

    @Indexed
    private String recipientRole;

    @Indexed
    private NotificationType type;

    private String title;
    private String message;

    @Builder.Default
    private String channel = "IN_APP";

    private String relatedEntityType;

    @Indexed
    private String relatedEntityId;

    @Indexed
    @Builder.Default
    private boolean read = false;

    private Instant readAt;

    private String createdBy;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Indexed
    private Instant createdAt;
}
