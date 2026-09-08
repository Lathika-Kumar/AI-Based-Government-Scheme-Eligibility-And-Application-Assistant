package com.schemebridge.scheme.dto.request;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.document.Scheme.FaqItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCreateRequest {
    
    @NotBlank(message = "Scheme code is required")
    private String schemeCode;

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotNull(message = "Multilingual title is required")
    private MultilingualText title;

    @NotNull(message = "Multilingual description is required")
    private MultilingualText description;

    private MultilingualText shortDescription;

    @NotBlank(message = "Category code is required")
    private String categoryCode;

    private String department;
    private String ministry;

    private String schemeLevel; // CENTRAL, STATE, UT
    private String stateOrUt;
    private String beneficiaryType;
    private String schemeType;

    private RuleGroup eligibilityRules;
    private List<SchemeBenefit> benefits;
    private List<RequiredDocument> requiredDocuments;
    private ApplicationInfo applicationInfo;
    
    @NotNull(message = "Source verification metadata is required")
    private SourceMetadata source;

    private List<String> tags;
    private List<FaqItem> faqs;

    private String status; // DRAFT, ACTIVE, etc.
    private Integer version;
}
