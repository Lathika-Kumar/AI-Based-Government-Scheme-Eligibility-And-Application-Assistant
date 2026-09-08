package com.schemebridge.scheme.document;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocument {
    private String documentCode; // AADHAAR, INCOME_CERTIFICATE, COMMUNITY_CERTIFICATE, BANK_ACCOUNT, etc.
    private MultilingualText name;
    private MultilingualText description;
    
    @Builder.Default
    private boolean mandatory = true;
    
    @Builder.Default
    private List<String> acceptedFormats = new ArrayList<>();
    
    private String issuingAuthority;
}
