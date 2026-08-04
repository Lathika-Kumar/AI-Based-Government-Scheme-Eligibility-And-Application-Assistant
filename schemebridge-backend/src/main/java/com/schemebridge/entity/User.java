package com.schemebridge.entity;

import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.VerificationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String fullName;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    @Builder.Default
    private Set<RoleEnum> roles = new HashSet<>();

    @Builder.Default
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @Builder.Default
    private Boolean enabled = true;

    private VerificationMethod verificationMethod;

    @Builder.Default
    private Boolean emailVerified = false;

    @Builder.Default
    private Boolean phoneVerified = false;

    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Builder.Default
    private Integer onboardingStep = 1;
}
