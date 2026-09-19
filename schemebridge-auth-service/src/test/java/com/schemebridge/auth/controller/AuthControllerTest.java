package com.schemebridge.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.auth.config.SecurityConfig;
import com.schemebridge.auth.dto.request.ChangePasswordRequest;
import com.schemebridge.auth.dto.request.LoginRequest;
import com.schemebridge.auth.dto.request.LogoutRequest;
import com.schemebridge.auth.dto.request.RefreshRequest;
import com.schemebridge.auth.dto.request.SignupRequest;
import com.schemebridge.auth.dto.request.VerifyOtpRequest;
import com.schemebridge.auth.dto.response.GenericMessageResponse;
import com.schemebridge.auth.dto.response.LoginResponse;
import com.schemebridge.auth.exception.LoginVerificationException;
import com.schemebridge.auth.dto.response.SignupResponse;
import com.schemebridge.auth.dto.response.TokenRefreshResponse;
import com.schemebridge.auth.dto.response.VerifyOtpResponse;
import com.schemebridge.auth.security.JwtTokenProvider;
import com.schemebridge.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private SignupRequest validRequest;

    @BeforeEach
    public void setUp() {
        validRequest = SignupRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("user@example.com")
                .password("StrongPassword123!")
                .phoneNumber("9876543210")
                .dob(java.time.LocalDate.of(2000, 1, 1))
                .build();
    }

    @Test
    public void testSignup_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        SignupResponse response = SignupResponse.builder()
                .message("Registration successful. Please verify your email.")
                .userId(1L)
                .email("user@example.com")
                .build();

        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.message").value("Registration successful. Please verify your email."));
    }

    @Test
    public void testSignup_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        validRequest.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }

    @Test
    public void testSignup_WeakPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        validRequest.setPassword("weak");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one special character"));
    }

    @Test
    public void testSignup_MissingFirstName_ReturnsBadRequest() throws Exception {
        // Arrange
        validRequest.setFirstName("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("First name is required"));
    }

    @Test
    public void testSignup_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        // Arrange
        validRequest.setPhoneNumber("12345"); // too short

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Phone number must be exactly 10 digits"));
    }

    @Test
    public void testVerifyOtp_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        VerifyOtpResponse response = VerifyOtpResponse.builder()
                .message("Email verified successfully. Your account is now active.")
                .build();

        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully. Your account is now active."));
    }

    @Test
    public void testVerifyOtp_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("invalid-email")
                .otp("123456")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }

    @Test
    public void testVerifyOtp_InvalidOtpFormat_ReturnsBadRequest() throws Exception {
        // Arrange
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("1234") // too short, must be exactly 6 digits
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("OTP must be exactly 6 digits"));
    }

    @Test
    public void testLogin_Success_ReturnsOk() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("StrongPassword123!")
                .build();

        LoginResponse response = LoginResponse.builder()
                .message("Login successful")
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(LoginResponse.UserInfoDto.builder()
                        .id(11L)
                        .email("user@example.com")
                        .roles(List.of("USER"))
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").value("mockAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(11))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"));
    }

    @Test
    public void testLogin_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email")
                .password("StrongPassword123!")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }

    @Test
    public void testLogin_MissingPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Password is required"));
    }

    @Test
    public void testRefresh_Success_ReturnsOk() throws Exception {
        // Arrange
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("mockRefreshToken")
                .build();

        TokenRefreshResponse response = TokenRefreshResponse.builder()
                .accessToken("mockNewAccessToken")
                .refreshToken("mockNewRefreshToken")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockNewAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("mockNewRefreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    public void testRefresh_MissingToken_ReturnsBadRequest() throws Exception {
        // Arrange
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Refresh token is required"));
    }

    @Test
    public void testLogout_Success_ReturnsOk() throws Exception {
        // Arrange
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("mockRefreshToken")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMe_NoJwt_ReturnsForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetMe_ValidJwt_ReturnsOk() throws Exception {
        // Arrange
        String validJwt = "valid.jwt.token";
        LoginResponse.UserInfoDto mockUser = LoginResponse.UserInfoDto.builder()
                .id(11L)
                .email("user@example.com")
                .roles(List.of("USER"))
                .build();

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validJwt)).thenReturn("11");
        when(jwtTokenProvider.getRolesFromToken(validJwt)).thenReturn(List.of("USER"));
        when(authService.getUserInfo(11L)).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    public void testGetMe_MalformedJwt_ReturnsForbidden() throws Exception {
        // Arrange
        String malformedJwt = "malformed-jwt";
        when(jwtTokenProvider.validateToken(malformedJwt)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header("Authorization", "Bearer " + malformedJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetMe_InvalidSignature_ReturnsForbidden() throws Exception {
        // Arrange
        String badSigJwt = "bad.signature.jwt";
        when(jwtTokenProvider.validateToken(badSigJwt)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header("Authorization", "Bearer " + badSigJwt))
                .andExpect(status().isForbidden());
    }

    // ── Change Password Tests ──────────────────────────────────────────────────

    @Test
    public void testChangePassword_NoJwt_ReturnsForbidden() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentPassword123!")
                .newPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testChangePassword_ValidRequest_ReturnsOk() throws Exception {
        String validJwt = "valid.jwt.token";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentPassword123!")
                .newPassword("NewPassword123!")
                .build();

        GenericMessageResponse mockResponse = GenericMessageResponse.builder()
                .message("Password changed successfully.")
                .build();

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validJwt)).thenReturn("11");
        when(jwtTokenProvider.getRolesFromToken(validJwt)).thenReturn(List.of("USER"));
        when(authService.changePassword(org.mockito.ArgumentMatchers.eq(11L), any(ChangePasswordRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + validJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));
    }

    @Test
    public void testChangePassword_InvalidCurrentPassword_ReturnsBadRequest() throws Exception {
        String validJwt = "valid.jwt.token";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123!")
                .newPassword("NewPassword123!")
                .build();

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validJwt)).thenReturn("11");
        when(jwtTokenProvider.getRolesFromToken(validJwt)).thenReturn(List.of("USER"));
        when(authService.changePassword(org.mockito.ArgumentMatchers.eq(11L), any(ChangePasswordRequest.class)))
                .thenThrow(new LoginVerificationException("Current password is incorrect"));

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + validJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    public void testChangePassword_ValidationFailure_WeakPassword_ReturnsBadRequest() throws Exception {
        String validJwt = "valid.jwt.token";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentPassword123!")
                .newPassword("weak") // fails pattern & length
                .build();

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validJwt)).thenReturn("11");
        when(jwtTokenProvider.getRolesFromToken(validJwt)).thenReturn(List.of("USER"));

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + validJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
