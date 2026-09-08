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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "application_documents")
@CompoundIndexes({
    @CompoundIndex(name = "app_doc_code_idx", def = "{'applicationId': 1, 'documentCode': 1}", unique = true)
})
public class ApplicationDocument {
    @Id
    private String id;

    @Indexed
    private String applicationId;

    @Indexed
    private String userId;

    @Indexed
    private String schemeCode;

    private String documentCode;
    private String documentName;
    private boolean mandatory;
    private boolean uploaded;

    private String fileName;
    private String contentType;
    private Long fileSize;
    private String storageReference;
    private String gridFsFileId;
    
    @Builder.Default
    private Integer version = 1;

    private Instant uploadedAt;
    private Instant verifiedAt;
    private Instant rejectedAt;
    private String verifiedBy;

    private String rejectionReason;

    @Indexed
    private DocumentVerificationStatus verificationStatus;

    @Indexed
    private DetailedDocumentStatus detailedStatus;

    @Builder.Default
    private List<ApplicationDocumentVersion> versionHistory = new ArrayList<>();
}
