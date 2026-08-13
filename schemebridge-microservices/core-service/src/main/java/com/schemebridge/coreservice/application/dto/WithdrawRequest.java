package com.schemebridge.coreservice.application.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {
    private String remarks;
    private String reason;
}
