package com.schemebridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultScoreResponse {
    private Integer completionPercentage;
    private Integer uploadedCount;
    private Integer missingCount;
    private Integer verifiedCount;
    private Integer pendingCount;
    private List<String> requiredDocumentTypes;
    private List<DocumentSummaryResponse> documents;
}
