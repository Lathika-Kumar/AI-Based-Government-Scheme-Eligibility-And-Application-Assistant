package com.schemebridge.documentservice.dto;

import com.schemebridge.documentservice.enums.DocumentType;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReadinessResponse {
    private String authUserId;
    private boolean isReadyForApplication;
    private double readinessPercentage;       // e.g. 80.0%
    private int totalUploaded;
    private int totalVerified;
    private List<DocumentType> uploadedTypes;
    private List<DocumentType> verifiedTypes;
    private List<DocumentType> missingEssentialTypes;
}
