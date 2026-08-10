package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerResponse {

    private Long id;
    private String officerId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private OfficerRole role;
    private OfficerStatus status;
    private String department;
    private String jurisdictionState;
    private String jurisdictionDistrict;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
