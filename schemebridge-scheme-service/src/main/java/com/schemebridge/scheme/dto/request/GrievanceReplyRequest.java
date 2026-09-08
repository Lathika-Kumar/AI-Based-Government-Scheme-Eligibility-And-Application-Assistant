package com.schemebridge.scheme.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrievanceReplyRequest {
    @NotBlank(message = "Message cannot be blank")
    private String message;
    private boolean internalOnly;
    private List<String> attachments;
}

