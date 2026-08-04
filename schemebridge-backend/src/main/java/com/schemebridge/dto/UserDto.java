package com.schemebridge.dto;

import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.VerificationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Boolean enabled;
    private VerificationMethod verificationMethod;
    private AccountStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean onboardingCompleted;
    private Set<RoleEnum> roles;
    private Instant createdAt;
}
