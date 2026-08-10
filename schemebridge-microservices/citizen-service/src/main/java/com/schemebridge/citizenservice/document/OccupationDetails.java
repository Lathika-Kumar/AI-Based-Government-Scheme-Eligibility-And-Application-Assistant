package com.schemebridge.citizenservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupationDetails {

    @Indexed
    private String occupationType;

    private String employmentStatus;
    private String employerType;
}
