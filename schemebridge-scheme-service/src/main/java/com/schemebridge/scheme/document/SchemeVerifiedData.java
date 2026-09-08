package com.schemebridge.scheme.document;

import com.schemebridge.scheme.enums.RequirementProvenance;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Phase 13 Canonical Scheme Knowledge Base Document.
 * Stored in MongoDB collection "scheme_verified_data" (isolated from master schemes collection).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "scheme_verified_data")
public class SchemeVerifiedData {

    @Id
    private String id;

    @Indexed(unique = true)
    private String schemeCode;

    @Indexed
    private String slug;

    private CanonicalSchemeIdentity identity;
    private CanonicalEligibility eligibility;
    
    @Builder.Default
    private List<CanonicalDocumentRequirement> documents = new ArrayList<>();
    
    @Builder.Default
    private List<CanonicalBenefit> benefits = new ArrayList<>();
    
    private CanonicalApplication application;
    private CanonicalSourceMetadata sourceMetadata;
    private ReconciliationResult reconciliation;
    private DataQualityMetrics qualityMetrics;

    @Indexed
    @Builder.Default
    private String documentStatus = "DOCUMENT_REQUIREMENTS_NOT_MAPPED"; // DOCUMENTS_FOUND, DOCUMENT_REQUIREMENTS_NOT_MAPPED, DOCUMENTS_EXPLICITLY_NOT_REQUIRED, SOURCE_NOT_FOUND

    @Indexed
    @Builder.Default
    private String reconciliationStatus = "UNRESOLVED"; // MATCHED, PARTIAL, UNMATCHED, AMBIGUOUS, CONFLICT

    @Indexed
    @Builder.Default
    private RequirementProvenance overallProvenance = RequirementProvenance.UNKNOWN_OR_UNSTRUCTURED;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalSchemeIdentity {
        private String schemeCode;
        private String schemeId;
        private String slug;
        private String schemeName;
        private MultilingualText title;
        private MultilingualText shortDescription;
        private MultilingualText detailedDescription;
        private String ministry;
        private String department;
        private SchemeLevel level;
        private String stateOrUt;
        private String category;
        private String beneficiaryType;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalEligibility {
        private String eligibilityText;
        private Map<String, Object> structuredEligibility;
        private String eligibilitySource;
        private RequirementProvenance eligibilityProvenance;
        private Instant eligibilityLastVerified;
        private String astAuthority;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalDocumentRequirement {
        private String documentCode;
        private String canonicalDocumentCode;
        private String officialDocumentName;
        private String description;
        private boolean mandatory;
        private boolean optional;
        private AlternativeGroup alternativeGroup;
        private String issuingAuthority;
        private List<String> acceptedFormats;
        private long maxSizeBytes;
        private String whyRequired;
        private RequirementProvenance provenance;
        private String sourceUrl;
        private String sourceEvidence;
        private Instant verifiedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeGroup {
        private String rule; // e.g. ONE_OF
        @Builder.Default
        private List<AlternativeOption> options = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlternativeOption {
        private String optionName;
        private String documentCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalBenefit {
        private String benefitId;
        private String benefitName;
        private String benefitType;
        private String benefitDescription;
        private Double amount;
        private String amountCurrency;
        private Double percentage;
        private String frequency;
        private String duration;
        private Double subsidy;
        private String otherBenefitMetadata;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalApplication {
        private String applicationMethod; // ONLINE, OFFLINE, BOTH
        private String applicationProcedure;
        @Builder.Default
        private List<String> applicationSteps = new ArrayList<>();
        private String applicationAuthority;
        private String officialApplicationUrl;
        private String officialPortalUrl;
        private String helplineNumber;
        private String offlineInstructions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CanonicalSourceMetadata {
        private String sourceType; // MYSCHEME_OFFICIAL_API, MYSCHEME_OFFICIAL_PAGE, GOVERNMENT_OFFICIAL_SOURCE, SYSTEM_CONFIGURED
        private String sourceUrl;
        private String sourceRecordId;
        private String sourceHash;
        private Instant extractedAt;
        private Instant lastVerifiedAt;
        private String dataVersion;
        private double confidence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReconciliationResult {
        private String status; // MATCHED, PARTIAL, UNMATCHED, AMBIGUOUS, CONFLICT
        @Builder.Default
        private List<String> matchedFields = new ArrayList<>();
        @Builder.Default
        private List<String> missingFields = new ArrayList<>();
        @Builder.Default
        private List<FieldConflict> conflictingFields = new ArrayList<>();
        private Instant lastReconciled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldConflict {
        private String fieldName;
        private Object existingValue;
        private Object officialValue;
        private String sourceUrl;
        private String conflictType;
        private String resolutionStatus; // UNRESOLVED, ACCEPT_OFFICIAL, RETAIN_EXISTING, MANUAL_REVIEW
        private Instant detectedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataQualityMetrics {
        private double completenessScore; // 0.0 to 1.0
        private double identityCompleteness;
        private double eligibilityCompleteness;
        private double documentCompleteness;
        private double benefitCompleteness;
        private double applicationCompleteness;
        @Builder.Default
        private List<String> missingFields = new ArrayList<>();
        @Builder.Default
        private List<String> warnings = new ArrayList<>();
        @Builder.Default
        private List<String> errors = new ArrayList<>();
    }
}
