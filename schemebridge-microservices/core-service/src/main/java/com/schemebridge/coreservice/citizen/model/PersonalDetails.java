package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalDetails {
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String religion;
    private String category;
    private String community;
    private String aadhaarMasked;
    private String panMasked;
}
