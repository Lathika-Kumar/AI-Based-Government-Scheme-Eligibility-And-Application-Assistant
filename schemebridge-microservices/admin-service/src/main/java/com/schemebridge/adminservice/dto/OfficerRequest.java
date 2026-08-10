package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email address is required")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Officer role is required")
    private OfficerRole role;

    @Builder.Default
    private OfficerStatus status = OfficerStatus.ACTIVE;

    private String department;
    private String jurisdictionState;
    private String jurisdictionDistrict;
}
