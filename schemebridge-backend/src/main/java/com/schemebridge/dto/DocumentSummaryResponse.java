package com.schemebridge.dto;

import com.schemebridge.enums.DocumentStatus;
import com.schemebridge.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSummaryResponse {
    private String id;
    private DocumentType documentType;
    private String documentName;
    private DocumentStatus status;
    private Instant uploadedAt;
    private Instant verifiedAt;
}
