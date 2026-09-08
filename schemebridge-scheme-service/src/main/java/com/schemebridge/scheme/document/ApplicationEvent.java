package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "application_events")
@CompoundIndexes({
    @CompoundIndex(name = "app_created_at_idx", def = "{'applicationId': 1, 'createdAt': 1}")
})
public class ApplicationEvent {
    @Id
    private String id;

    @Indexed
    private String applicationId;

    @Indexed
    private String userId;

    private ApplicationEventType eventType;

    private ApplicationStatus fromStatus;

    private ApplicationStatus toStatus;

    private String message;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    private Map<String, Object> metadata;
}
