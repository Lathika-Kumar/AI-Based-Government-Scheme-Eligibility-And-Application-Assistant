package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeDetails {

    @Indexed
    private BigDecimal annualIncome;

    private String incomeSource;
    private Boolean taxPayer;
}
