package com.schemebridge.dto;

import com.schemebridge.enums.RoleEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserRoleRequest {
    @NotNull(message = "Role is required")
    private RoleEnum role;
}
