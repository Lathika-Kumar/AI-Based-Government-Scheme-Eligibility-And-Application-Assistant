package com.schemebridge.scheme.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeDraftResponse {

    private String name;
    private String schemeCode;
    private String description;
    private String shortDescription;
    private String ministry;
    private String department;
    private String category;
    private String schemeLevel; // CENTRAL, STATE, UT
    private String stateOrUt;
    private String beneficiaryType;

    // Eligibility Draft Criteria
    private Integer minAge;
    private Integer maxAge;
    private Double maxIncome;
    @Builder.Default
    private List<String> occupations = new ArrayList<>();
    @Builder.Default
    private List<String> castes = new ArrayList<>();
    @Builder.Default
    private List<String> genders = new ArrayList<>();
    @Builder.Default
    private List<String> states = new ArrayList<>();

    @Builder.Default
    private List<String> benefits = new ArrayList<>();
    @Builder.Default
    private List<String> requiredDocuments = new ArrayList<>();
    @Builder.Default
    private List<String> steps = new ArrayList<>();

    private String officialLink;
    private String deadline;

    // Traceability Metadata
    private String sourceDocumentId;
    private String sourceFileName;
    private String uploadedBy;
    private Instant uploadedAt;
    private Instant extractedAt;
    private String modelUsed;
    private String extractionStatus; // SUCCESS, MANUAL_REVIEW, FAILED
    private String rawSnippet;
}
