package com.schemebridge.authservice.controller;

import com.schemebridge.authservice.dto.request.*;
import com.schemebridge.authservice.dto.response.AuthResponse;
import com.schemebridge.authservice.dto.response.TokenResponse;
import com.schemebridge.authservice.dto.response.UserSummaryResponse;
import com.schemebridge.authservice.service.AuthService;
import com.schemebridge.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Identity", description = "Endpoints for Registration, Login, JWT Tokens, OTP Verification, and Password Management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "User Registration", description = "Creates a new user account in PENDING_VERIFICATION state and dispatches email OTP")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserSummaryResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Verification OTP code sent to email", response));
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials, logs login audit, manages brute-force account locking, and returns JWT Access & Refresh Tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh JWT Access Token", description = "Validates & rotates refresh token to issue a new short-lived JWT Access Token")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Access token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Revokes refresh token and invalidates active security session")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/send-email-otp")
    @Operation(summary = "Send Email Verification OTP", description = "Generates a 6-digit OTP code and dispatches email via Brevo REST API")
    public ResponseEntity<ApiResponse<Void>> sendEmailOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email OTP sent successfully"));
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify Email OTP", description = "Verifies 6-digit OTP code and transitions user status to ACTIVE")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyEmailOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. Account is now ACTIVE"));
    }

    @PostMapping("/send-phone-otp")
    @Operation(summary = "Send Phone Verification OTP", description = "Generates phone OTP verification record")
    public ResponseEntity<ApiResponse<Void>> sendPhoneOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendPhoneOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Phone OTP sent successfully"));
    }

    @PostMapping("/verify-phone-otp")
    @Operation(summary = "Verify Phone OTP", description = "Verifies phone OTP code and marks phone as verified")
    public ResponseEntity<ApiResponse<Void>> verifyPhoneOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyPhoneOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Phone verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password Request", description = "Generates password reset OTP code and dispatches email via Brevo")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent to email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password with OTP", description = "Verifies password reset OTP, re-hashes password with BCrypt, unlocks account, and revokes active tokens")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Please log in with your new password"));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate Token Signature", description = "Internal validation endpoint checking JWT signature and returning user identity claims")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        UserSummaryResponse response = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
    }
}
