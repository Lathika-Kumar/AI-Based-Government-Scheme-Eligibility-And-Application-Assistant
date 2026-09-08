package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Real interaction event logged when an authenticated citizen interacts with a scheme.
 * Serves as genuine behavioral evidence for future offline Learning-to-Rank (LTR).
 */
@Document(collection = "recommendation_events")
@CompoundIndexes({
    @CompoundIndex(name = "user_time_idx", def = "{'userId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "scheme_time_idx", def = "{'schemeCode': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "event_model_idx", def = "{'eventType': 1, 'recommendationContext.modelVersion': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEvent {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String schemeCode;

    @Indexed
    private RecommendationEventType eventType;

    private RecommendationContextSnapshot recommendationContext;

    @Indexed
    private Instant timestamp;

    private String sessionId;

    private Map<String, Object> metadata;
}
