package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReadiness {
    private Integer uploadedDocumentCount;
    private Integer requiredDocumentCount;
    private Integer verifiedDocuments;
    private Integer pendingDocuments;
    private Integer missingDocuments;
    private Double readinessPercentage;
}
