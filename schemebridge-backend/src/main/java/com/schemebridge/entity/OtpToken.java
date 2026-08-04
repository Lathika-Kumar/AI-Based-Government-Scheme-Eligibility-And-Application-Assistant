package com.schemebridge.entity;

import com.schemebridge.enums.VerificationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "otp_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String code;
    private VerificationMethod type;
    private Instant expiryTime;

    @Builder.Default
    private Boolean used = false;

    @Builder.Default
    private Integer verificationAttempts = 0;

    @Builder.Default
    private Integer resendCount = 0;

    private Instant lastSentAt;
}
