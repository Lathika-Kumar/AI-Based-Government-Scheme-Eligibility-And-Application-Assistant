package com.schemebridge.coreservice.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiExplanationRequest {

    @NotBlank(message = "Scheme ID or scheme code is required")
    private String schemeId;

    @Builder.Default
    private String language = "en"; // "en" or "ta"
}
