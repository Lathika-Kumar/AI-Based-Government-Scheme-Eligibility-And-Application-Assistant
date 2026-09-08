package com.schemebridge.auth.controller;

import com.schemebridge.auth.dto.request.ForgotPasswordRequest;
import com.schemebridge.auth.dto.request.LoginRequest;
import com.schemebridge.auth.dto.request.LogoutRequest;
import com.schemebridge.auth.dto.request.RefreshRequest;
import com.schemebridge.auth.dto.request.ResendOtpRequest;
import com.schemebridge.auth.dto.request.ResetPasswordRequest;
import com.schemebridge.auth.dto.request.SignupRequest;
import com.schemebridge.auth.dto.request.VerifyOtpRequest;
import com.schemebridge.auth.dto.response.GenericMessageResponse;
import com.schemebridge.auth.dto.response.LoginResponse;
import com.schemebridge.auth.dto.response.SignupResponse;
import com.schemebridge.auth.dto.response.TokenRefreshResponse;
import com.schemebridge.auth.dto.response.VerifyOtpResponse;
import com.schemebridge.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for User Registration and Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Registers a new citizen with PENDING_VERIFICATION status and generates an email verification OTP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = SignupResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation parameters failed",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email is already registered",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP for user registration", description = "Verifies the email OTP. Activates user account on success.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = VerifyOtpResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or validation failure",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifyOtp(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies email, password, active status, and email verification status. Returns access token, refresh token, and user details.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials, unverified account, or inactive status",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token", description = "Rotates and issues a new access token and a new refresh token using a valid, active refresh token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token rotation successful",
            content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid, expired, or revoked refresh token",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenRefreshResponse response = authService.refresh(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke session refresh token", description = "Revokes the supplied refresh token to prevent further rotation.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Invalid refresh token",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Returns details of the currently authenticated user based on the Bearer JWT token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved authenticated user",
            content = @Content(schema = @Schema(implementation = LoginResponse.UserInfoDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse.UserInfoDto> getMe() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String userIdStr = (String) authentication.getPrincipal();
        Long userId = Long.valueOf(userIdStr);
        LoginResponse.UserInfoDto userInfo = authService.getUserInfo(userId);
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }

    // ── Forgot Password ────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Initiate password recovery",
        description = "Sends a 6-digit OTP to the registered email if it exists. " +
                      "Always returns HTTP 200 regardless of whether the email is registered " +
                      "to prevent account enumeration. OTP expires in 15 minutes.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Generic confirmation message",
            content = @Content(schema = @Schema(implementation = GenericMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<GenericMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        GenericMessageResponse response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password (atomic)",
        description = "Atomically verifies the PASSWORD_RESET OTP and updates the password " +
                      "in a single server-side transaction. On success, revokes all existing " +
                      "refresh token sessions. The OTP and new password must be submitted together; " +
                      "no client-side 'OTP verified' state is trusted.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful",
            content = @Content(schema = @Schema(implementation = GenericMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid/expired OTP, too many attempts, or weak password",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<GenericMessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        GenericMessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    // ── OTP Resend ────────────────────────────────────────────────────────────

    @PostMapping("/resend-otp")
    @Operation(
        summary = "Resend email verification OTP",
        description = "Resends an email verification OTP for the signup flow. " +
                      "Enforces a 60-second cooldown and invalidates the previous OTP. " +
                      "Always returns HTTP 200 regardless of whether the email is registered.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Generic confirmation message",
            content = @Content(schema = @Schema(implementation = GenericMessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class))),
        @ApiResponse(responseCode = "429", description = "Resend too soon — cooldown in effect",
            content = @Content(schema = @Schema(implementation = com.schemebridge.auth.dto.response.ErrorResponse.class)))
    })
    public ResponseEntity<GenericMessageResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        GenericMessageResponse response = authService.resendOtp(request.getEmail());
        return ResponseEntity.ok(response);
    }
}
