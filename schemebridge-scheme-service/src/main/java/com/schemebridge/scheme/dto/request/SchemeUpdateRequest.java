package com.schemebridge.scheme.dto.request;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.document.Scheme.FaqItem;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeUpdateRequest {
    
    @NotNull(message = "Multilingual title is required")
    private MultilingualText title;

    @NotNull(message = "Multilingual description is required")
    private MultilingualText description;

    private MultilingualText shortDescription;

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
    private SourceMetadata source;

    private List<String> tags;
    private List<FaqItem> faqs;

    private String status;
    private Integer version;
}
