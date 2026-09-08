package com.schemebridge.auth.service;

import com.schemebridge.auth.dto.request.ResetPasswordRequest;
import com.schemebridge.auth.dto.response.GenericMessageResponse;
import com.schemebridge.auth.entity.*;
import com.schemebridge.auth.exception.OtpVerificationException;
import com.schemebridge.auth.exception.RateLimitException;
import com.schemebridge.auth.repository.OtpVerificationRepository;
import com.schemebridge.auth.repository.RefreshTokenRepository;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import com.schemebridge.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Hardening Batch 1 additions:
 *   - forgotPassword (anti-enumeration, OTP saved with PASSWORD_RESET purpose)
 *   - resetPassword (atomic: expiry, attempt limit, correct OTP, session revocation)
 *   - resendOtp (cooldown, previous OTP invalidation, anti-enumeration)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Hardening Batch 1 (Password Reset & OTP Resend)")
class AuthServiceHardeningTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private OtpVerificationRepository otpVerificationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;


    private Role userRole;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "refreshExpirationInMs", 604800000L);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "logRawOtp", true);
        userRole = Role.builder().id(1L).name("USER").description("Standard user").build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Forgot Password
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Unknown email returns same generic message (anti-enumeration)")
        void unknownEmail_returnsGenericMessage_noOtpSaved() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            GenericMessageResponse response = authService.forgotPassword("unknown@test.com");

            assertNotNull(response);
            assertTrue(response.getMessage().contains("If an account with that email exists"));
            verify(otpVerificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Known email saves hashed PASSWORD_RESET OTP and returns generic message")
        void knownEmail_savesHashedOtp_returnsGenericMessage() {
            User user = activeUser(20L, "forgot@test.com");
            when(userRepository.findByEmail("forgot@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.expirePreviousOtps(any(), any(), any(), any())).thenReturn(0);
            when(passwordEncoder.encode(any())).thenReturn("hashed-reset-otp");

            GenericMessageResponse response = authService.forgotPassword("forgot@test.com");

            // Same generic message as unknown email
            assertTrue(response.getMessage().contains("If an account with that email exists"));

            // OTP saved with correct purpose
            ArgumentCaptor<OtpVerification> cap = ArgumentCaptor.forClass(OtpVerification.class);
            verify(otpVerificationRepository).save(cap.capture());
            assertEquals(OtpPurpose.PASSWORD_RESET, cap.getValue().getPurpose());
            assertEquals("hashed-reset-otp", cap.getValue().getOtpHash());
            assertTrue(cap.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(10)));
        }

        @Test
        @DisplayName("Previous PASSWORD_RESET OTPs are invalidated before issuing new one")
        void previousOtps_areInvalidated_beforeNewOtp() {
            User user = activeUser(21L, "prev@test.com");
            when(userRepository.findByEmail("prev@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(any())).thenReturn("h");
            when(otpVerificationRepository.expirePreviousOtps(any(), any(), any(), any())).thenReturn(1);

            authService.forgotPassword("prev@test.com");

            verify(otpVerificationRepository)
                    .expirePreviousOtps(eq(user), eq(OtpPurpose.PASSWORD_RESET), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Reset Password (Atomic)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resetPassword() — atomic OTP verification + password update + session revocation")
    class ResetPasswordTests {

        @Test
        @DisplayName("Invalid OTP increments attempts and does NOT update password or revoke sessions")
        void invalidOtp_doesNotUpdatePassword() {
            User user = activeUser(21L, "reset@test.com");
            OtpVerification otp = resetOtp(user);

            when(userRepository.findByEmail("reset@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(otp));
            when(passwordEncoder.matches("wrong", otp.getOtpHash())).thenReturn(false);

            ResetPasswordRequest req = req("reset@test.com", "wrong", "NewPass456!");
            assertThrows(OtpVerificationException.class, () -> authService.resetPassword(req));

            assertEquals(1, otp.getAttempts());
            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllActiveTokensForUser(any());
        }

        @Test
        @DisplayName("Expired OTP throws OtpVerificationException")
        void expiredOtp_throwsException() {
            User user = activeUser(22L, "exp@test.com");
            OtpVerification otp = resetOtp(user);
            otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(userRepository.findByEmail("exp@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(otp));

            ResetPasswordRequest req = req("exp@test.com", "123456", "NewPass456!");
            OtpVerificationException ex = assertThrows(OtpVerificationException.class,
                    () -> authService.resetPassword(req));
            assertTrue(ex.getMessage().contains("expired"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Already-consumed OTP throws OtpVerificationException")
        void alreadyUsedOtp_throwsException() {
            User user = activeUser(23L, "used@test.com");
            OtpVerification otp = resetOtp(user);
            otp.setVerifiedAt(LocalDateTime.now().minusMinutes(5));

            when(userRepository.findByEmail("used@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(otp));

            ResetPasswordRequest req = req("used@test.com", "any", "NewPass789!");
            OtpVerificationException ex = assertThrows(OtpVerificationException.class,
                    () -> authService.resetPassword(req));
            assertTrue(ex.getMessage().contains("already been used"));
            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).revokeAllActiveTokensForUser(any());
        }

        @Test
        @DisplayName("Success: updates BCrypt password, marks OTP consumed, revokes ALL sessions atomically")
        void success_updatesPassword_revokesAllSessions() {
            User user = activeUser(24L, "ok@test.com");
            OtpVerification otp = resetOtp(user);

            when(userRepository.findByEmail("ok@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(otp));
            when(passwordEncoder.matches("correct", otp.getOtpHash())).thenReturn(true);
            when(passwordEncoder.matches("NewPass456!", user.getPasswordHash())).thenReturn(false);
            when(passwordEncoder.encode("NewPass456!")).thenReturn("new-bcrypt-hash");
            when(refreshTokenRepository.revokeAllActiveTokensForUser(user)).thenReturn(3);

            ResetPasswordRequest req = req("ok@test.com", "correct", "NewPass456!");
            GenericMessageResponse response = authService.resetPassword(req);

            assertTrue(response.getMessage().contains("Password has been reset"));
            assertEquals("new-bcrypt-hash", user.getPasswordHash()); // password updated
            verify(userRepository).save(user);
            assertNotNull(otp.getVerifiedAt()); // OTP marked consumed
            verify(refreshTokenRepository).revokeAllActiveTokensForUser(user); // sessions revoked
        }

        @Test
        @DisplayName("Exceeding max attempts locks out further attempts")
        void tooManyAttempts_throwsException() {
            User user = activeUser(25L, "locked@test.com");
            OtpVerification otp = resetOtp(user);
            otp.setAttempts(3); // already at max

            when(userRepository.findByEmail("locked@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(otp));

            ResetPasswordRequest req = req("locked@test.com", "any", "NewPass789!");
            OtpVerificationException ex = assertThrows(OtpVerificationException.class,
                    () -> authService.resetPassword(req));
            assertTrue(ex.getMessage().contains("Too many"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Resend OTP
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resendOtp()")
    class ResendOtpTests {

        @Test
        @DisplayName("Within 60s cooldown throws RateLimitException and saves no OTP")
        void withinCooldown_throwsRateLimitException() {
            User user = pendingUser(30L, "resend@test.com");
            OtpVerification recent = emailOtp(user);
            recent.setCreatedAt(LocalDateTime.now().minusSeconds(30)); // within cooldown

            when(userRepository.findByEmail("resend@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(recent));

            RateLimitException ex = assertThrows(RateLimitException.class,
                    () -> authService.resendOtp("resend@test.com"));
            assertTrue(ex.getMessage().contains("60 seconds"));
            verify(otpVerificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("After cooldown: invalidates previous OTPs and saves new hashed OTP")
        void afterCooldown_invalidatesPrevious_savesNew() {
            User user = pendingUser(31L, "resend2@test.com");
            OtpVerification old = emailOtp(user);
            old.setCreatedAt(LocalDateTime.now().minusSeconds(90)); // past cooldown

            when(userRepository.findByEmail("resend2@test.com")).thenReturn(Optional.of(user));
            when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                    .thenReturn(Optional.of(old));
            when(otpVerificationRepository.expirePreviousOtps(any(), any(), any(), any())).thenReturn(1);
            when(passwordEncoder.encode(any())).thenReturn("new-hashed-otp");

            GenericMessageResponse response = authService.resendOtp("resend2@test.com");
            assertTrue(response.getMessage().contains("new OTP has been sent"));

            // Previous OTPs expired
            verify(otpVerificationRepository)
                    .expirePreviousOtps(eq(user), eq(OtpPurpose.EMAIL_VERIFICATION), any(), any());

            // New OTP saved
            ArgumentCaptor<OtpVerification> cap = ArgumentCaptor.forClass(OtpVerification.class);
            verify(otpVerificationRepository).save(cap.capture());
            assertEquals(OtpPurpose.EMAIL_VERIFICATION, cap.getValue().getPurpose());
            assertEquals("new-hashed-otp", cap.getValue().getOtpHash());
        }

        @Test
        @DisplayName("Unknown email returns generic message without saving OTP (anti-enumeration)")
        void unknownEmail_genericMessage_noOtpSaved() {
            when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
            GenericMessageResponse response = authService.resendOtp("nobody@test.com");
            assertTrue(response.getMessage().contains("new OTP has been sent"));
            verify(otpVerificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Already-verified user returns generic message without saving OTP")
        void alreadyVerified_genericMessage_noOtpSaved() {
            User user = activeUser(32L, "verified@test.com"); // emailVerified=true
            when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(user));
            GenericMessageResponse response = authService.resendOtp("verified@test.com");
            assertTrue(response.getMessage().contains("new OTP has been sent"));
            verify(otpVerificationRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private User activeUser(Long id, String email) {
        return User.builder().id(id).firstName("T").lastName("U").email(email)
                .passwordHash("existing-hash").accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).roles(Collections.singleton(userRole)).build();
    }

    private User pendingUser(Long id, String email) {
        return User.builder().id(id).firstName("T").lastName("U").email(email)
                .passwordHash("existing-hash").accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false).roles(Collections.singleton(userRole)).build();
    }

    private OtpVerification resetOtp(User user) {
        OtpVerification otp = OtpVerification.builder().id(100L).user(user)
                .otpHash("hashed-otp").purpose(OtpPurpose.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(15)).attempts(0).build();
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        return otp;
    }

    private OtpVerification emailOtp(User user) {
        OtpVerification otp = OtpVerification.builder().id(101L).user(user)
                .otpHash("hashed-email-otp").purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).attempts(0).build();
        otp.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        return otp;
    }

    private ResetPasswordRequest req(String email, String otp, String newPassword) {
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setEmail(email);
        r.setOtp(otp);
        r.setNewPassword(newPassword);
        return r;
    }
}
