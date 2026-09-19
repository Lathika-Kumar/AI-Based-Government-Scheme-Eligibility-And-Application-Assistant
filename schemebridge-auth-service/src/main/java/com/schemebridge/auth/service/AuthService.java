package com.schemebridge.auth.service;

import com.schemebridge.auth.dto.request.ChangePasswordRequest;
import com.schemebridge.auth.dto.request.LoginRequest;
import com.schemebridge.auth.dto.request.LogoutRequest;
import com.schemebridge.auth.dto.request.RefreshRequest;
import com.schemebridge.auth.dto.request.ResetPasswordRequest;
import com.schemebridge.auth.dto.request.SignupRequest;
import com.schemebridge.auth.dto.request.VerifyOtpRequest;
import com.schemebridge.auth.dto.response.GenericMessageResponse;
import com.schemebridge.auth.dto.response.LoginResponse;
import com.schemebridge.auth.dto.response.SignupResponse;
import com.schemebridge.auth.dto.response.TokenRefreshResponse;
import com.schemebridge.auth.dto.response.VerifyOtpResponse;
import com.schemebridge.auth.entity.*;
import com.schemebridge.auth.exception.DuplicateEmailException;
import com.schemebridge.auth.exception.LoginVerificationException;
import com.schemebridge.auth.exception.OtpVerificationException;
import com.schemebridge.auth.exception.RateLimitException;
import com.schemebridge.auth.repository.OtpVerificationRepository;
import com.schemebridge.auth.repository.RefreshTokenRepository;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import com.schemebridge.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    @Value("${otp.log-raw-value:false}")
    private boolean logRawOtp;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationInMs;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();


    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // Normalize email
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered");
        }

        // Fetch default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found in database"));

        // Create User entity
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(normalizedEmail)
                .dob(request.getDob())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .roles(Collections.singleton(userRole))
                .build();

        // Save User
        User savedUser = userRepository.save(user);

        // Generate Secure 6-digit OTP
        String rawOtp = generateSixDigitOtp();

        // Log OTP clearly for local testing/development only if enabled
        if (logRawOtp) {
            log.info("[TEST-ONLY] Generated OTP for user {}: {}", normalizedEmail, rawOtp);
        }

        // Save OTP Verification record
        OtpVerification otpVerification = OtpVerification.builder()
                .user(savedUser)
                .otpHash(passwordEncoder.encode(rawOtp)) // Using BCrypt hash
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // Expires in 10 minutes
                .attempts(0)
                .build();

        otpVerificationRepository.save(otpVerification);

        // Dispatch OTP via Email Service
        emailService.sendEmailVerificationOtp(normalizedEmail, savedUser.getFirstName(), rawOtp);

        log.info("User registered successfully: userId={}, email={}", savedUser.getId(), normalizedEmail);


        return SignupResponse.builder()
                .message("Registration successful. Please verify your email.")
                .userId(savedUser.getId())
                .email(normalizedEmail)
                .build();
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Find user by email
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new OtpVerificationException("User not found"));

        // 2. Check if already verified
        if (user.isEmailVerified() || user.getAccountStatus() == AccountStatus.ACTIVE) {
            return VerifyOtpResponse.builder()
                    .message("Email is already verified and account is active")
                    .build();
        }

        // 3. Find the latest OTP verification record
        OtpVerification otpVerification = otpVerificationRepository
                .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new OtpVerificationException("No verification request found for this email"));

        // 4. Check if already verified (in OTP verification record)
        if (otpVerification.getVerifiedAt() != null) {
            throw new OtpVerificationException("This OTP has already been verified");
        }

        // 5. Check if expired
        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException("OTP has expired");
        }

        // 6. Check if exceeded maximum attempts (e.g. 3 attempts limit)
        if (otpVerification.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new OtpVerificationException("Too many verification attempts. Please request a new OTP.");
        }

        // 7. Verify OTP hash
        if (!passwordEncoder.matches(request.getOtp(), otpVerification.getOtpHash())) {
            // Increment attempts
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            otpVerificationRepository.save(otpVerification);
            
            int remaining = MAX_OTP_ATTEMPTS - otpVerification.getAttempts();
            if (remaining <= 0) {
                throw new OtpVerificationException("Invalid OTP. Too many verification attempts. Please request a new OTP.");
            } else {
                throw new OtpVerificationException("Invalid OTP. " + remaining + " attempts remaining.");
            }
        }

        // 8. Success: Update OTP Verification Record
        otpVerification.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otpVerification);

        // 9. Success: Update User Status
        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", normalizedEmail);

        return VerifyOtpResponse.builder()
                .message("Email verified successfully. Your account is now active.")
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Find user by email
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new LoginVerificationException("Invalid email or password"));

        // 2. Verify BCrypt password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new LoginVerificationException("Invalid email or password");
        }

        // 3. Verify email verification
        if (!user.isEmailVerified()) {
            throw new LoginVerificationException("Email verification required");
        }

        // 4. Verify account status
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new LoginVerificationException("Account is not active");
        }

        // 5. Load user roles
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // 6. Generate JWT access token
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);

        // 7. Generate secure refresh token
        String rawRefreshToken = generateSecureToken();
        String tokenHash = hashToken(rawRefreshToken);

        // 8. Persist refresh token session
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("User logged in successfully: userId={}, email={}", user.getId(), normalizedEmail);

        return LoginResponse.builder()
                .message("Login successful")
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(LoginResponse.UserInfoDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phoneNumber(user.getPhoneNumber())
                        .dob(user.getDob())
                        .roles(roles)
                        .build())
                .build();
    }

    @Transactional
    public TokenRefreshResponse refresh(RefreshRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        // 1. Find token
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new LoginVerificationException("Invalid refresh token"));

        // 2. Check revoked
        if (refreshToken.isRevoked()) {
            throw new LoginVerificationException("Refresh token has been revoked");
        }

        // 3. Check expiration
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new LoginVerificationException("Refresh token expired");
        }

        // 4. Load user
        User user = refreshToken.getUser();

        // 5. Verify account still ACTIVE
        if (user.getAccountStatus() != AccountStatus.ACTIVE || !user.isEmailVerified()) {
            throw new LoginVerificationException("Account is not active");
        }

        // 6. Refresh Token Rotation: Revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // 7. Generate new access token
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), roles);

        // 8. Generate and persist new refresh token
        String newRawRefreshToken = generateSecureToken();
        String newHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("Refresh token rotated successfully for userId={}", user.getId());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String rawToken = request.getRefreshToken();
        String tokenHash = hashToken(rawToken);

        // Find token and revoke
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new LoginVerificationException("Invalid refresh token"));

        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked during logout for userId={}", refreshToken.getUser().getId());
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse.UserInfoDto getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LoginVerificationException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return LoginResponse.UserInfoDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dob(user.getDob())
                .roles(roles)
                .build();
    }

    /**
     * Authenticated change password.
     * Verifies the user's current password against the stored BCrypt hash,
     * validates that the new password differs from the current password,
     * and persists the newly hashed password.
     */
    @Transactional
    public GenericMessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LoginVerificationException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new LoginVerificationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new LoginVerificationException("New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for userId={}", userId);

        return GenericMessageResponse.builder()
                .message("Password changed successfully.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Forgot Password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiates password recovery.
     * Always returns a generic message regardless of whether the email is registered
     * to prevent account enumeration attacks.
     */
    @Transactional
    public GenericMessageResponse forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // Expire all outstanding unverified PASSWORD_RESET OTPs for this user
            otpVerificationRepository.expirePreviousOtps(
                    user,
                    OtpPurpose.PASSWORD_RESET,
                    LocalDateTime.now(),
                    LocalDateTime.now().minusSeconds(1));

            String rawOtp = generateSixDigitOtp();
            if (logRawOtp) {
                log.info("[TEST-ONLY] Password-reset OTP for user {}: {}", normalizedEmail, rawOtp);
            }

            OtpVerification otp = OtpVerification.builder()
                    .user(user)
                    .otpHash(passwordEncoder.encode(rawOtp))
                    .purpose(OtpPurpose.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .attempts(0)
                    .build();
            otpVerificationRepository.save(otp);

            // Dispatch Password Reset OTP (silently log failures to preserve anti-enumeration)
            try {
                emailService.sendPasswordResetOtp(normalizedEmail, user.getFirstName(), rawOtp);
            } catch (Exception ex) {
                log.error("Failed to deliver password reset email for userId={}: {}", user.getId(), ex.getMessage());
            }

            log.info("Password-reset OTP issued for userId={}", user.getId());

        });

        // Always return same message — do NOT reveal whether the email is registered
        return GenericMessageResponse.builder()
                .message("If an account with that email exists, a password reset code has been sent.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset Password (Atomic)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atomically:
     * 1. Verifies the PASSWORD_RESET OTP
     * 2. Validates the new password
     * 3. Updates the BCrypt password hash
     * 4. Marks the OTP as consumed
     * 5. Revokes ALL active refresh tokens for the user
     *
     * No client-side "OTP verified" state is trusted — this is a single atomic
     * operation. The OTP and the new password are submitted together.
     */
    @Transactional(noRollbackFor = OtpVerificationException.class)
    public GenericMessageResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Find user — use a deliberately vague error to avoid enumeration
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new OtpVerificationException("Invalid or expired reset code."));

        // 2. Find latest PASSWORD_RESET OTP
        OtpVerification otpVerification = otpVerificationRepository
                .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new OtpVerificationException("Invalid or expired reset code."));

        // 3. Check already consumed
        if (otpVerification.getVerifiedAt() != null) {
            throw new OtpVerificationException("This reset code has already been used.");
        }

        // 4. Check expiry
        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException("Reset code has expired. Please request a new one.");
        }

        // 5. Check attempt limit
        if (otpVerification.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new OtpVerificationException("Too many incorrect attempts. Please request a new reset code.");
        }

        // 6. Verify OTP hash
        if (!passwordEncoder.matches(request.getOtp(), otpVerification.getOtpHash())) {
            otpVerification.setAttempts(otpVerification.getAttempts() + 1);
            otpVerificationRepository.save(otpVerification);
            int remaining = MAX_OTP_ATTEMPTS - otpVerification.getAttempts();
            if (remaining <= 0) {
                throw new OtpVerificationException("Invalid reset code. Too many incorrect attempts. Please request a new reset code.");
            }
            throw new OtpVerificationException("Invalid reset code. " + remaining + " attempts remaining.");
        }

        // 7. Validate new password (must not be identical to current password)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new OtpVerificationException("New password must be different from the current password.");
        }

        // 8. Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 9. Mark OTP as consumed
        otpVerification.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otpVerification);

        // 10. Revoke ALL active refresh tokens (invalidate all existing sessions)
        int revokedCount = refreshTokenRepository.revokeAllActiveTokensForUser(user);
        log.info("Password reset successful for userId={}. {} active session(s) revoked.", user.getId(), revokedCount);

        return GenericMessageResponse.builder()
                .message("Password has been reset successfully. Please log in with your new password.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resend OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resends an email verification OTP for the signup flow.
     *
     * Security rules:
     * - 60-second cooldown enforced via createdAt of the most-recent OTP
     * - Previous unverified OTPs are expired (set expiresAt to past) so they
     *   cannot be reused after a new OTP is issued
     * - Always returns a generic message (anti-enumeration)
     */
    @Transactional
    public GenericMessageResponse resendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // If already verified, silently succeed
            if (user.isEmailVerified()) {
                return;
            }

            // Check cooldown: latest OTP must have been created > 60s ago
            otpVerificationRepository
                    .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION)
                    .ifPresent(latest -> {
                        if (latest.getVerifiedAt() == null
                                && latest.getCreatedAt() != null
                                && latest.getCreatedAt().isAfter(
                                        LocalDateTime.now().minusSeconds(OTP_RESEND_COOLDOWN_SECONDS))) {
                            throw new RateLimitException(
                                    "Please wait " + OTP_RESEND_COOLDOWN_SECONDS
                                    + " seconds before requesting another OTP.");
                        }
                    });

            // Expire all previous unverified EMAIL_VERIFICATION OTPs
            otpVerificationRepository.expirePreviousOtps(
                    user,
                    OtpPurpose.EMAIL_VERIFICATION,
                    LocalDateTime.now(),
                    LocalDateTime.now().minusSeconds(1));

            // Generate and save new OTP
            String rawOtp = generateSixDigitOtp();
            if (logRawOtp) {
                log.info("[TEST-ONLY] Resent OTP for user {}: {}", normalizedEmail, rawOtp);
            }

            OtpVerification newOtp = OtpVerification.builder()
                    .user(user)
                    .otpHash(passwordEncoder.encode(rawOtp))
                    .purpose(OtpPurpose.EMAIL_VERIFICATION)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .attempts(0)
                    .build();
            otpVerificationRepository.save(newOtp);

            // Dispatch Resent OTP via Email Service
            emailService.sendEmailVerificationOtp(normalizedEmail, user.getFirstName(), rawOtp);

            log.info("OTP resent for userId={}", user.getId());
        });

        // Always return generic message
        return GenericMessageResponse.builder()
                .message("If your email is pending verification, a new OTP has been sent.")
                .build();
    }

    private String generateSixDigitOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
