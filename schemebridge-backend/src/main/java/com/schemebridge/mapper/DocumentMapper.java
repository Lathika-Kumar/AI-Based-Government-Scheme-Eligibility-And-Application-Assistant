package com.schemebridge.mapper;

import com.schemebridge.dto.DocumentRequest;
import com.schemebridge.dto.DocumentResponse;
import com.schemebridge.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toDocumentResponse(Document document) {
        if (document == null) return null;

        return DocumentResponse.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .name(document.getName())
                .type(document.getType())
                .issuer(document.getIssuer())
                .expiryDate(document.getExpiryDate())
                .source(document.getSource())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .linkedSchemes(document.getLinkedSchemes())
                .rejectionReason(document.getRejectionReason())
                .verifiedAt(document.getVerifiedAt())
                .uploadedAt(document.getUploadedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public Document toEntity(DocumentRequest request, String userId) {
        if (request == null) return null;

        return Document.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .issuer(request.getIssuer())
                .expiryDate(request.getExpiryDate())
                .source(request.getSource())
                .linkedSchemes(request.getLinkedSchemes())
                .uploadedAt(java.time.Instant.now())
                .status(com.schemebridge.enums.DocumentStatus.UPLOADED)
                .build();
    }
}
