package com.schemebridge.dto;

import com.schemebridge.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private String id;
    private String userId;
    private String name;
    private String type;
    private String issuer;
    private String expiryDate;
    private String source;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;
    private List<String> linkedSchemes;
    private String rejectionReason;
    private Instant verifiedAt;
    private Instant uploadedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
