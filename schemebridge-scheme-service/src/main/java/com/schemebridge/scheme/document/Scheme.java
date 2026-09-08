package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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
@Document(collection = "schemes")
public class Scheme {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String schemeCode;
    
    @Indexed(unique = true)
    private String slug;
    
    private MultilingualText title;
    private MultilingualText description;
    private MultilingualText shortDescription;
    
    private SchemeCategoryRef category;
    
    private String department;
    private String ministry;
    
    @Indexed
    private SchemeLevel schemeLevel; // CENTRAL, STATE, UT
    
    @Indexed
    private String stateOrUt;
    
    @Indexed
    private String beneficiaryType;
    
    private String schemeType;
    
    private RuleGroup eligibilityRules;
    
    @Builder.Default
    private List<SchemeBenefit> benefits = new ArrayList<>();
    
    @Builder.Default
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();
    
    private ApplicationInfo applicationInfo;
    private SourceMetadata source;
    
    @Indexed
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    @Builder.Default
    private List<FaqItem> faqs = new ArrayList<>();
    
    @Indexed
    @Builder.Default
    private SchemeStatus status = SchemeStatus.DRAFT;
    
    @Builder.Default
    private int version = 1;
    
    private Instant lastVerifiedAt;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FaqItem {
        private String question;
        private String answer;
    }
}
