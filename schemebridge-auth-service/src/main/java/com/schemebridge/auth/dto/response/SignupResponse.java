package com.schemebridge.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {
    private String message;
    private Long userId;
    private String email;
}
