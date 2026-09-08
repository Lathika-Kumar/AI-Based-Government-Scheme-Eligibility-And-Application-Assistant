package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDocumentChecklistResponse {
    private String schemeCode;
    private String schemeTitle;
    private String slug;
    private String officialSourceUrl;
    private String officialSourceName;
    private String sourceLastUpdated;
    private String reconciliationStatus;
    private String documentStatus; // DOCUMENTS_FOUND, DOCUMENT_REQUIREMENTS_NOT_MAPPED, DOCUMENTS_EXPLICITLY_NOT_REQUIRED, SOURCE_NOT_FOUND
    private String overallProvenance; // VERIFIED_OFFICIAL, SYSTEM_CONFIGURED, UNKNOWN_OR_UNSTRUCTURED
    private String applicationId;
    private String applicationNumber;
    private String applicationStatus;
    private boolean canSubmit;
    private boolean canApprove;
    private int totalDocuments;
    private int totalRequired;
    private int mandatoryDocumentCount;
    private int alternativeGroupCount;
    private int totalUploaded;
    private int totalVerified;
    private int totalRejected;
    private List<DocumentChecklistItemResponse> items;
    private List<String> missingRequirements;
    private List<String> eligibilityConditions;
    private List<String> applicationSteps;
    private String applicationMode;
    private String officialApplicationUrl;
    private String helplineNumber;
    private List<String> benefits;
}
