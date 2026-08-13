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
public class AiDocumentSuggestionsResponse {

    private String schemeId;
    private String schemeName;
    private String language;

    private int totalRequiredCount;
    private int availableCount;
    private int missingCount;
    private int pendingCount;
    private int rejectedCount;

    @Builder.Default
    private List<DocumentStatusItem> availableDocuments = new ArrayList<>();

    @Builder.Default
    private List<DocumentStatusItem> missingDocuments = new ArrayList<>();

    @Builder.Default
    private List<DocumentStatusItem> pendingDocuments = new ArrayList<>();

    @Builder.Default
    private List<DocumentStatusItem> rejectedDocuments = new ArrayList<>();

    private String suggestion;

    @Builder.Default
    private List<String> suggestedNextSteps = new ArrayList<>();

    private boolean isFallback;
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentStatusItem {
        private String documentName;
        private String documentType;
        private boolean mandatory;
        private String status; // VERIFIED, MISSING, PENDING, REJECTED, EXPIRED
        private String remarks;
    }
}
