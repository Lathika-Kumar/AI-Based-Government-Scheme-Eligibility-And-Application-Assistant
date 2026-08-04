package com.schemebridge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "citizen_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenProfile extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String maritalStatus;

    private String state;
    private String district;
    private String cityOrVillage;
    private String pincode;

    private BigDecimal annualIncome;
    private String category;
    private String occupation;
    private String employmentStatus;

    @Builder.Default
    private Boolean disabilityStatus = false;

    private String qualification;

    // Sensitive Identity Documents (masked when exposed via DTO)
    private String aadhaarNumber;
    private String panNumber;

    // Profile Versioning & Audit
    @Builder.Default
    private Integer profileVersion = 1;

    private LocalDateTime lastUpdatedAt;
    private String lastUpdatedBy;
    private LocalDateTime profileCompletedAt;

    // Soft Delete Preparedness
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;
    private String deletedBy;
}
