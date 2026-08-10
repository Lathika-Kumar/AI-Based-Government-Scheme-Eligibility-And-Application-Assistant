package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyDetails {
    private Integer familyMembersCount;
    private Integer dependentsCount;
    private String rationCardType;
}
