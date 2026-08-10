package com.schemebridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private String message;
    private String path;
    @Builder.Default
    private Instant timestamp = Instant.now();
    private List<String> errors;

    public static ErrorResponse of(String message, String path, List<String> errors) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }

    public static ErrorResponse of(String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}
