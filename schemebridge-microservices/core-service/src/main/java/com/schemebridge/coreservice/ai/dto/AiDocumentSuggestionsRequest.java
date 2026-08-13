package com.schemebridge.coreservice.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentSuggestionsRequest {

    private String schemeId;

    @Builder.Default
    private String language = "en";
}
