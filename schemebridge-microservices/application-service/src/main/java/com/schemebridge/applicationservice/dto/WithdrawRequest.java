package com.schemebridge.applicationservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {
    private String remarks;
    private String reason;
}
