package com.schemebridge.scheme.document;

import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeBenefit {
    private MultilingualText title;
    private MultilingualText description;
    
    private BigDecimal amount;
    private String amountType; // FINANCIAL, SUBSIDY, SCHOLARSHIP, INSURANCE, SERVICE, TRAINING, EMPLOYMENT, HOUSING, OTHER
    
    private String frequency; // ONE_TIME, MONTHLY, ANNUALLY, etc.
    private String duration;
    
    @Builder.Default
    private List<String> conditions = new ArrayList<>();
}
