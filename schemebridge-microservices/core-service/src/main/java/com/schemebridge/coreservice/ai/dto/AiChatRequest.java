package com.schemebridge.coreservice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    private String schemeId;

    @NotBlank(message = "Message cannot be blank")
    private String message;

    @Builder.Default
    private String language = "en";

    private String conversationId;
}
