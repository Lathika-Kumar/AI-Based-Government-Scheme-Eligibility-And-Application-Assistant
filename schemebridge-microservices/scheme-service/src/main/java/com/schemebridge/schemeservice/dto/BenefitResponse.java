package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenefitResponse {
    private String id;
    private String benefitType;
    private String title;
    private String descriptionEnglish;
    private String descriptionTamil;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String frequency;
    private String currency;
    private Integer displayOrder;
}
