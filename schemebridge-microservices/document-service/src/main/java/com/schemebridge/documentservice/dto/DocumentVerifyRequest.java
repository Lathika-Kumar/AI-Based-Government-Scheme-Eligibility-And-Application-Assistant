package com.schemebridge.documentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVerifyRequest {

    @NotBlank(message = "Verified by (Officer ID) is required")
    private String verifiedBy;

    private String verifiedByName;
    private String remarks;
    private String rejectionReason;
}
