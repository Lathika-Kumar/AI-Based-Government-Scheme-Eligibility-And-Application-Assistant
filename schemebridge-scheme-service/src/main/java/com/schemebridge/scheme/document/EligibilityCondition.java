package com.schemebridge.scheme.document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityCondition {
    private String field;
    private RuleOperator operator;
    private String value;
    private String dataType; // e.g. NUMBER, STRING, BOOLEAN, DATE
    
    private MultilingualText question;
    private MultilingualText description;
    
    @Builder.Default
    private boolean required = true;
    
    private String sourceReference;
}
