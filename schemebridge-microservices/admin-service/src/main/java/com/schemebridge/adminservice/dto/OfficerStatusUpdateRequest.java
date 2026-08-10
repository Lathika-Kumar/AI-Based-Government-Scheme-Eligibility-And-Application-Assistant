package com.schemebridge.adminservice.dto;

import com.schemebridge.adminservice.enums.OfficerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OfficerStatus status;
}
