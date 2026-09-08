package com.schemebridge.scheme.dto.response;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.document.Scheme.FaqItem;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeResponse {
    private String id;
    private String schemeCode;
    private String slug;
    private MultilingualText title;
    private MultilingualText description;
    private MultilingualText shortDescription;
    private SchemeCategoryRef category;
    private String department;
    private String ministry;
    private SchemeLevel schemeLevel;
    private String stateOrUt;
    private String beneficiaryType;
    private String schemeType;
    private RuleGroup eligibilityRules;
    private List<SchemeBenefit> benefits;
    private List<RequiredDocument> requiredDocuments;
    private ApplicationInfo applicationInfo;
    private SourceMetadata source;
    private List<String> tags;
    private List<FaqItem> faqs;
    private SchemeStatus status;
    private int version;
    private Instant lastVerifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
