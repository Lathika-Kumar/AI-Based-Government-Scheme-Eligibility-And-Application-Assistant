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
@Document(collection = "admin_audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "actor_created_idx", def = "{'actorUserId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "entity_created_idx", def = "{'entityType': 1, 'entityId': 1, 'createdAt': -1}")
})
public class AdminAuditLog {

    @Id
    private String id;

    @Indexed
    private String actorUserId;

    private String actorRole;

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    private Object beforeState;
    private Object afterState;

    private String ipAddress;
    private String userAgent;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Indexed
    private Instant createdAt;
}
