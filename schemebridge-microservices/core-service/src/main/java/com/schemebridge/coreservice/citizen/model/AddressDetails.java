package com.schemebridge.coreservice.citizen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDetails {
    private String line1;
    private String line2;
    private String village;
    private String taluk;

    @Indexed
    private String district;

    @Indexed
    private String state;

    private String pincode;
}
