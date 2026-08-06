package com.schemebridge.dto;

import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserUpdateRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private AccountStatus status;
    private Set<RoleEnum> roles;
}
