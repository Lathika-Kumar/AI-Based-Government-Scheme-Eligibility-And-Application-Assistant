package com.schemebridge.scheme.document;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleGroup {
    
    @Builder.Default
    private String logicalOperator = "ALL"; // ALL, ANY, NOT
    
    @Builder.Default
    private List<EligibilityCondition> conditions = new ArrayList<>();
    
    @Builder.Default
    private List<RuleGroup> groups = new ArrayList<>();

    private String rawText;
}
