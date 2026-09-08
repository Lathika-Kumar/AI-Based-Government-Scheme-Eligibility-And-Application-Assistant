package com.schemebridge.scheme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String response;

    private String conversationId;

    private List<String> suggestions;

    private List<Map<String, Object>> relatedSchemes;

    private Map<String, String> actionLink;

    private Map<String, Object> metadata;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
