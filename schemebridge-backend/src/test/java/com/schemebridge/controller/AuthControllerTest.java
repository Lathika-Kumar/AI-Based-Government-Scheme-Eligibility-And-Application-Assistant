package com.schemebridge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.dto.ForgotPasswordRequest;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.dto.ResetPasswordRequest;
import com.schemebridge.dto.SendOtpRequest;
import com.schemebridge.dto.VerifyOtpRequest;
import com.schemebridge.entity.OtpToken;
import com.schemebridge.entity.User;
import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.VerificationMethod;
import com.schemebridge.repository.OtpTokenRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.email.EmailProvider;
import com.schemebridge.service.email.SimulatedEmailProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailProvider emailProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Test
    @DisplayName("Sprint 1 Integration Test: Registration (PENDING_VERIFICATION), Email OTP, Verification (ACTIVE), and Login")
    void testCompleteSprint1AuthAndOtpLifecycle() throws Exception {
        String testEmail = "sprint1_citizen_" + System.currentTimeMillis() + "@schemebridge.gov.in";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(testEmail)
                .password("SecurePass123!")
                .fullName("Lathika Sprint1")
                .phoneNumber("9876543210")
                .role(RoleEnum.CITIZEN)
                .build();

        // 1. Register User -> Status PENDING_VERIFICATION
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testEmail))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // 2. Send Email OTP
        SendOtpRequest sendOtpReq = SendOtpRequest.builder()
                .email(testEmail)
                .build();

        MvcResult sendOtpResult = mockMvc.perform(post("/api/v1/auth/send-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipient").value(testEmail))
                .andExpect(jsonPath("$.data.method").value("EMAIL"))
                .andExpect(jsonPath("$.data.expirySeconds").value(300))
                .andExpect(jsonPath("$.data.remainingVerificationAttempts").value(5))
                .andExpect(jsonPath("$.data.resendCount").value(0))
                .andReturn();

        String sendOtpJson = sendOtpResult.getResponse().getContentAsString();
        String simulatedOtp;

        if (emailProvider instanceof SimulatedEmailProvider) {
            simulatedOtp = objectMapper.readTree(sendOtpJson).path("data").path("simulatedOtp").asText();
            assertThat(simulatedOtp).isNotBlank();
        } else {
            assertThat(objectMapper.readTree(sendOtpJson).path("data").has("simulatedOtp")).isFalse();
            User registered = userRepository.findByEmail(testEmail)
                    .or(() -> userRepository.findByEmailIgnoreCase(testEmail))
                    .orElseThrow(() -> new AssertionError("Registered user not found for test email"));
            OtpToken token = otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByLastSentAtDesc(
                    registered.getId(), VerificationMethod.EMAIL
            ).orElseThrow(() -> new AssertionError("OTP token not found for registered user"));
            simulatedOtp = token.getCode();
        }

        // 3. Attempt Invalid OTP Verification -> 400 Bad Request
        VerifyOtpRequest invalidOtpReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp("000000")
                .verificationMethod(VerificationMethod.EMAIL)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 4. Verify Valid OTP -> Status transitions to ACTIVE
        VerifyOtpRequest validOtpReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp(simulatedOtp)
                .verificationMethod(VerificationMethod.EMAIL)
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        // 5. Login Authenticated User
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("SecurePass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));

        // 6. Actuator Health
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
