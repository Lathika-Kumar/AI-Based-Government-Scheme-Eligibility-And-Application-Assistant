package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.ApplicationDocumentVersion;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChecklistItemResponse {
    private String documentCode;
    private String canonicalDocumentCode;
    private String documentName;
    private String document;
    private String description;
    private String whyRequired;
    private String reason;
    private boolean mandatory;
    private boolean required;
    private boolean optional;
    private String sourceRequirement;
    private String statusString;
    private boolean userProvided;
    private String alternativeGroupId;
    private String alternativeGroupType; // e.g. ONE_OF
    private List<String> alternatives;
    private List<String> acceptedFormats;
    private long maxSizeBytes;
    private String issuingAuthority;
    private RequirementProvenance provenance;
    private String officialSourceUrl;
    private String sourceReference;
    private Instant sourceLastVerified;
    private DetailedDocumentStatus status;
    private boolean uploaded;
    private String fileName;
    private Integer version;
    private Instant uploadedAt;
    private boolean ocrAvailable;
    private boolean ocrCompleted;
    private String ocrStatus;
    private Double ocrConfidence;
    private String verificationStatus;
    private boolean verified;
    private String rejectionReason;
    private String verifiedBy;
    private Instant verifiedAt;
    private boolean canSubmit;
    private boolean canApprove;
    private boolean alternativeSatisfied;
    private List<ApplicationDocumentVersion> versionHistory;
    private String source;
    private String vaultDocumentId;
    private String identityMatchStatus;
    private boolean satisfied;
    public String getDocument() {
        return document != null ? document : documentName;
    }

    public String getReason() {
        return reason != null ? reason : whyRequired;
    }

    public String getSourceRequirement() {
        return sourceRequirement != null ? sourceRequirement : (sourceReference != null ? sourceReference : "Statutory requirement published in government scheme guidelines");
    }

    public String getStatusString() {
        if (statusString != null) return statusString;
        if (uploaded || verified || (status != null && status != DetailedDocumentStatus.NOT_UPLOADED)) {
            return "PROVIDED";
        }
        return "MISSING";
    }
}
