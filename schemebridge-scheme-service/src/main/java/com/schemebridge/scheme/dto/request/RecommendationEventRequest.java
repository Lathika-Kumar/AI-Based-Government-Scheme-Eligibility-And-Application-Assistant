package com.schemebridge.scheme.dto.request;

import com.schemebridge.scheme.document.RecommendationEventType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

/**
 * Request payload for logging genuine citizen interaction events.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationEventRequest {

    @NotBlank(message = "schemeCode is required")
    private String schemeCode;

    @NotNull(message = "eventType is required")
    private RecommendationEventType eventType;

    @Min(value = 1, message = "recommendationRank must be >= 1")
    private Integer recommendationRank;

    @DecimalMin(value = "0.0", message = "recommendationScore must be >= 0.0")
    @DecimalMax(value = "1.0", message = "recommendationScore must be <= 1.0")
    private Double recommendationScore;

    private String sessionId;

    private Map<String, Object> metadata;
}
