package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.AuthResponse;
import com.schemebridge.dto.ForgotPasswordRequest;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.OtpResponse;
import com.schemebridge.dto.RefreshTokenRequest;
import com.schemebridge.dto.RefreshTokenResponse;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.dto.ResetPasswordRequest;
import com.schemebridge.dto.SendOtpRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.dto.VerifyOtpRequest;
import com.schemebridge.service.AuthService;
import com.schemebridge.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, logout, OTP verification, and password recovery")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with status PENDING_VERIFICATION.")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDto registeredUser = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", registeredUser));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and returns a JWT access token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.loginUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/send-email-otp")
    @Operation(summary = "Send Email OTP", description = "Generates a 6-digit Email OTP valid for 5 minutes.")
    public ResponseEntity<ApiResponse<OtpResponse>> sendEmailOtp(@Valid @RequestBody SendOtpRequest request) {
        OtpResponse response = otpService.sendEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email OTP sent successfully", response));
    }

    @PostMapping("/send-phone-otp")
    @Operation(summary = "Send Phone OTP", description = "Stores phone number and generates a 6-digit Mobile OTP valid for 5 minutes.")
    public ResponseEntity<ApiResponse<OtpResponse>> sendPhoneOtp(@Valid @RequestBody SendOtpRequest request) {
        OtpResponse response = otpService.sendPhoneOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Mobile OTP sent successfully", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP code", description = "Validates 6-digit OTP, updates user status to ACTIVE, and instantiates CitizenProfile.")
    public ResponseEntity<ApiResponse<UserDto>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        UserDto verifiedUser = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Account verified successfully", verifiedUser));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Refreshes an expired access token using a valid refresh token.")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse refreshed = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", refreshed));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Clears user authentication context.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logoutUser();
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password recovery", description = "Generates password reset token placeholder for the given email.")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String result = authService.initiateForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using the provided reset token.")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String result = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
