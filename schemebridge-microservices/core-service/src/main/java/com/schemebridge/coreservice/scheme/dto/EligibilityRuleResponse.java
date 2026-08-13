package com.schemebridge.coreservice.scheme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityRuleResponse {
    private String id;
    private String ruleType;
    private String operator;
    private String valueString;
    private BigDecimal valueNumberMin;
    private BigDecimal valueNumberMax;
    private String description;
    private Boolean mandatory;
    private Integer displayOrder;
}
