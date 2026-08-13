package com.schemebridge.coreservice.document.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAuditTrailItem {
    private String action;           // UPLOAD, VERIFY, REJECT, DELETE, REUPLOAD
    private String performedBy;      // User ID or Officer ID
    private String performedByName;
    private String ipAddress;
    private String remarks;
    private LocalDateTime timestamp;
}
