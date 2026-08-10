package com.schemebridge.applicationservice.document;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocumentRef {
    private String documentId;         // Future: Document Service ID
    private String documentType;
    private String documentName;
    private String fileUrl;
    private String fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
    private String verificationStatus;  // PENDING, VERIFIED, REJECTED
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
}
