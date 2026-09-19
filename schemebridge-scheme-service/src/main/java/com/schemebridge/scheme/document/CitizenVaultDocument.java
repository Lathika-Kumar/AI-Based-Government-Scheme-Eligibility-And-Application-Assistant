package com.schemebridge.scheme.document;

import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Citizen-owned permanent document repository entity.
 * Persists documents across sessions, schemes, and applications in MongoDB GridFS.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "citizen_vault_documents")
@CompoundIndexes({
    @CompoundIndex(name = "user_doc_code_idx", def = "{'userId': 1, 'documentCode': 1}"),
    @CompoundIndex(name = "user_doc_type_idx", def = "{'userId': 1, 'documentType': 1}")
})
public class CitizenVaultDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String documentCode;

    private String canonicalDocumentCode;

    private String documentName;

    private String documentType;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String storageReference;

    private String gridFsFileId;

    @Builder.Default
    private Integer version = 1;

    private String sha256;

    @Builder.Default
    private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING;

    @Builder.Default
    private DetailedDocumentStatus detailedStatus = DetailedDocumentStatus.UPLOADED;

    private String aiVerificationResult;

    private Double verificationScore;

    @Builder.Default
    private boolean officerVerified = false;

    private String verifiedBy;

    private Instant verifiedAt;

    private String rejectionReason;

    private String issuer;

    private String expiryDate;

    private String holderName;

    private String docNumber;

    private String extractedHolderName;

    private String extractedDob;

    private String extractedDocNumber;

    @Builder.Default
    private String identityMatchStatus = "NOT_CHECKED"; // "MATCH", "MISMATCH", "NOT_CHECKED"

    @Builder.Default
    private String extractionStatus = "SUCCESS"; // "SUCCESS", "PARTIAL", "UNCERTAIN", "FAILED"

    @Builder.Default
    private String source = "Manual Upload";

    @Builder.Default
    private String provider = "LOCAL";

    private Instant uploadedAt;

    private Instant updatedAt;

    @Builder.Default
    private List<String> linkedApplications = new ArrayList<>();

    @Builder.Default
    private List<ApplicationDocumentVersion> versionHistory = new ArrayList<>();
}
