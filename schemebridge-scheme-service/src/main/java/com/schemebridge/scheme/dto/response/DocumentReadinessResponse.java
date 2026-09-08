package com.schemebridge.scheme.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReadinessResponse {
    private int total;
    private int uploaded;
    private int mandatoryMissing;
    private int verified;
    private int rejected;
    private int percentage;
}
