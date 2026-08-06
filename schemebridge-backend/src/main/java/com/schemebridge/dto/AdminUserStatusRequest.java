package com.schemebridge.dto;

import com.schemebridge.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserStatusRequest {
    @NotNull(message = "Status is required")
    private AccountStatus status;
}
