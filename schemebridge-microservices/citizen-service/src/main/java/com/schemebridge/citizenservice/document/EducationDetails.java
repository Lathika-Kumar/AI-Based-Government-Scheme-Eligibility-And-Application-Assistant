package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDetails {
    private String qualification;
    private String stream;
    private Integer completionYear;
}
