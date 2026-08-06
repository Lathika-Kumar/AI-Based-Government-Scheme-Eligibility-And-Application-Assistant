package com.schemebridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentMetadataResponse {
    private String documentId;
    private String provider;
    private String remoteDocumentId;
    private String source;
    private Instant syncedAt;
    private String status;
}
