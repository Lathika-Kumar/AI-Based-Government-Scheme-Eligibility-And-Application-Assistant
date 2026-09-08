package com.schemebridge.scheme.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectDocumentRequest {
    @NotBlank(message = "Rejection reason is required.")
    private String reason;
}
