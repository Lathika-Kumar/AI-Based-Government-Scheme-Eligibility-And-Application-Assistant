package com.schemebridge.entity;

import com.schemebridge.enums.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;
    private String type;
    private String issuer;
    private String expiryDate;
    private String source;
    private String fileName;
    private Long fileSize;

    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Builder.Default
    private List<String> linkedSchemes = new ArrayList<>();

    private String rejectionReason;
    private Instant verifiedAt;
    private Instant uploadedAt;
}
