package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDetails {
    private String email;
    private String phone;
    private String altPhone;
}
