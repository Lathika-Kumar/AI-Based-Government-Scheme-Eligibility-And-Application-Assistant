package com.schemebridge.coreservice.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String conversationId;
    private String schemeId;
    private String message;
    private String language;
    private boolean isFallback;

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private Instant timestamp;
}
