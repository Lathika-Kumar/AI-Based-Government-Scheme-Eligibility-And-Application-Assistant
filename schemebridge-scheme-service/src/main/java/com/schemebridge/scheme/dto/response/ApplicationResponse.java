package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.MultilingualText;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private String id;
    private String applicationNumber;
    private String userId;
    private String schemeCode;
    private MultilingualText schemeTitle;
    private String status;
    private Instant submittedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private DocumentReadinessResponse documentReadiness;
    private List<ApplicationDocumentResponse> documents;
}
