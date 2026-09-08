package com.schemebridge.auth.service;

import com.schemebridge.auth.dto.request.LoginRequest;
import com.schemebridge.auth.dto.request.LogoutRequest;
import com.schemebridge.auth.dto.request.RefreshRequest;
import com.schemebridge.auth.dto.request.SignupRequest;
import com.schemebridge.auth.dto.request.VerifyOtpRequest;
import com.schemebridge.auth.dto.response.LoginResponse;
import com.schemebridge.auth.dto.response.SignupResponse;
import com.schemebridge.auth.dto.response.TokenRefreshResponse;
import com.schemebridge.auth.dto.response.VerifyOtpResponse;
import com.schemebridge.auth.entity.*;
import com.schemebridge.auth.exception.DuplicateEmailException;
import com.schemebridge.auth.exception.LoginVerificationException;
import com.schemebridge.auth.exception.OtpVerificationException;
import com.schemebridge.auth.repository.OtpVerificationRepository;
import com.schemebridge.auth.repository.RefreshTokenRepository;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import com.schemebridge.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;


    private Role userRole;
    private SignupRequest signupRequest;

    @BeforeEach
    public void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "refreshExpirationInMs", 604800000L);
        org.springframework.test.util.ReflectionTestUtils.setField(authService, "logRawOtp", true);

        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .description("Standard user")
                .build();

        signupRequest = SignupRequest.builder()

                .firstName("Test")
                .lastName("User")
                .email("user@example.com")
                .password("StrongPassword123!")
                .phoneNumber("9876543210")
                .build();
    }

    @Test
    public void testSignup_Success() {
        // Arrange
        String rawPassword = signupRequest.getPassword();
        String hashedPassword = "hashedPassword123";

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(any(String.class))).thenAnswer(invocation -> {
            String arg = invocation.getArgument(0);
            if (arg.equals(rawPassword)) {
                return hashedPassword;
            }
            return "hashedOtpCode";
        });

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        // Act
        SignupResponse response = authService.signup(signupRequest);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getUserId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Registration successful. Please verify your email.", response.getMessage());

        // Verify User parameters saved
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Test", savedUser.getFirstName());
        assertEquals("User", savedUser.getLastName());
        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals(hashedPassword, savedUser.getPasswordHash());
        assertEquals("9876543210", savedUser.getPhoneNumber());
        assertEquals(AccountStatus.PENDING_VERIFICATION, savedUser.getAccountStatus());
        assertFalse(savedUser.isEmailVerified());
        assertTrue(savedUser.getRoles().contains(userRole));

        // Verify OTP parameters saved
        ArgumentCaptor<OtpVerification> otpCaptor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(otpVerificationRepository).save(otpCaptor.capture());
        OtpVerification savedOtp = otpCaptor.getValue();
        assertEquals(savedUser, savedOtp.getUser());
        assertEquals("hashedOtpCode", savedOtp.getOtpHash());
        assertEquals(OtpPurpose.EMAIL_VERIFICATION, savedOtp.getPurpose());
        assertNotNull(savedOtp.getExpiresAt());
        assertEquals(0, savedOtp.getAttempts());
    }

    @Test
    public void testSignup_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(otpVerificationRepository, never()).save(any(OtpVerification.class));
    }

    @Test
    public void testSignup_MissingRole_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> authService.signup(signupRequest));
        verify(userRepository, never()).save(any(User.class));
        verify(otpVerificationRepository, never()).save(any(OtpVerification.class));
    }

    @Test
    public void testVerifyOtp_Success() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        OtpVerification otp = OtpVerification.builder()
                .id(10L)
                .user(user)
                .otpHash("hashedOtpCode")
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attempts(0)
                .build();

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hashedOtpCode")).thenReturn(true);

        // Act
        VerifyOtpResponse response = authService.verifyOtp(request);

        // Assert
        assertNotNull(response);
        assertEquals("Email verified successfully. Your account is now active.", response.getMessage());
        assertTrue(user.isEmailVerified());
        assertEquals(AccountStatus.ACTIVE, user.getAccountStatus());
        assertNotNull(otp.getVerifiedAt());
        verify(userRepository).save(user);
        verify(otpVerificationRepository).save(otp);
    }

    @Test
    public void testVerifyOtp_InvalidOtp_IncrementsAttempts() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        OtpVerification otp = OtpVerification.builder()
                .id(10L)
                .user(user)
                .otpHash("hashedOtpCode")
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attempts(0)
                .build();

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hashedOtpCode")).thenReturn(false);

        // Act & Assert
        OtpVerificationException exception = assertThrows(OtpVerificationException.class, 
                () -> authService.verifyOtp(request));
        
        assertTrue(exception.getMessage().contains("Invalid OTP"));
        assertEquals(1, otp.getAttempts());
        verify(otpVerificationRepository).save(otp);
        verify(userRepository, never()).save(user);
    }

    @Test
    public void testVerifyOtp_ExpiredOtp_ThrowsException() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        OtpVerification otp = OtpVerification.builder()
                .id(10L)
                .user(user)
                .otpHash("hashedOtpCode")
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired
                .attempts(0)
                .build();

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otp));

        // Act & Assert
        OtpVerificationException exception = assertThrows(OtpVerificationException.class, 
                () -> authService.verifyOtp(request));
        
        assertEquals("OTP has expired", exception.getMessage());
        verify(otpVerificationRepository, never()).save(otp);
    }

    @Test
    public void testVerifyOtp_MaxAttemptsReached_ThrowsException() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        OtpVerification otp = OtpVerification.builder()
                .id(10L)
                .user(user)
                .otpHash("hashedOtpCode")
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .attempts(3) // Already 3 attempts
                .build();

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("user@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(otpVerificationRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otp));

        // Act & Assert
        OtpVerificationException exception = assertThrows(OtpVerificationException.class, 
                () -> authService.verifyOtp(request));
        
        assertEquals("Too many verification attempts. Please request a new OTP.", exception.getMessage());
        verify(otpVerificationRepository, never()).save(otp);
    }

    @Test
    public void testVerifyOtp_UserNotFound_ThrowsException() {
        // Arrange
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nonexistent@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        OtpVerificationException exception = assertThrows(OtpVerificationException.class, 
                () -> authService.verifyOtp(request));
        
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testLogin_Success() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .roles(Collections.singleton(userRole))
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("otpfixed01@example.com")
                .password("StrongPassword123!")
                .build();

        when(userRepository.findByEmail("otpfixed01@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(eq(11L), eq("otpfixed01@example.com"), anyList())).thenReturn("jwtAccessToken");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("jwtAccessToken", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals(11L, response.getUser().getId());
        assertEquals("otpfixed01@example.com", response.getUser().getEmail());
        assertTrue(response.getUser().getRoles().contains("USER"));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    public void testLogin_InvalidPassword_ThrowsException() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("otpfixed01@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("otpfixed01@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    public void testLogin_UnverifiedEmail_ThrowsException() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false) // Unverified
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("otpfixed01@example.com")
                .password("StrongPassword123!")
                .build();

        when(userRepository.findByEmail("otpfixed01@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "hashedPassword")).thenReturn(true);

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.login(request));
        assertEquals("Email verification required", exception.getMessage());
    }

    @Test
    public void testLogin_InactiveAccount_ThrowsException() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .passwordHash("hashedPassword")
                .accountStatus(AccountStatus.LOCKED) // Inactive
                .emailVerified(true)
                .build();

        LoginRequest request = LoginRequest.builder()
                .email("otpfixed01@example.com")
                .password("StrongPassword123!")
                .build();

        when(userRepository.findByEmail("otpfixed01@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPassword123!", "hashedPassword")).thenReturn(true);

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.login(request));
        assertEquals("Account is not active", exception.getMessage());
    }

    @Test
    public void testLogin_UnknownEmail_ThrowsException() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .password("StrongPassword123!")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    public void testRefresh_Success() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .roles(Collections.singleton(userRole))
                .build();

        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("someOldTokenHash")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("oldRawRefreshToken")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));
        when(jwtTokenProvider.generateToken(eq(11L), eq("otpfixed01@example.com"), anyList())).thenReturn("newAccessToken");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(3600L);

        // Act
        TokenRefreshResponse response = authService.refresh(request);

        // Assert
        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertTrue(oldToken.isRevoked());
        verify(refreshTokenRepository).save(oldToken);
        verify(refreshTokenRepository).save(argThat(newToken -> !newToken.isRevoked() && newToken.getUser().equals(user)));
    }

    @Test
    public void testRefresh_RevokedToken_ThrowsException() {
        // Arrange
        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .tokenHash("someOldTokenHash")
                .revoked(true) // Revoked
                .build();

        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("oldRawRefreshToken")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.refresh(request));
        assertEquals("Refresh token has been revoked", exception.getMessage());
    }

    @Test
    public void testRefresh_ExpiredToken_ThrowsException() {
        // Arrange
        RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .tokenHash("someOldTokenHash")
                .expiresAt(LocalDateTime.now().minusSeconds(1)) // Expired
                .revoked(false)
                .build();

        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("oldRawRefreshToken")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.refresh(request));
        assertEquals("Refresh token expired", exception.getMessage());
    }

    @Test
    public void testRefresh_InvalidToken_ThrowsException() {
        // Arrange
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("invalidRefreshToken")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.refresh(request));
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    public void testLogout_Success() {
        // Arrange
        User user = User.builder().id(11L).build();
        RefreshToken token = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("someTokenHash")
                .revoked(false)
                .build();

        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("rawRefreshToken")
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        // Act
        authService.logout(request);

        // Assert
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    public void testGetUserInfo_Success() {
        // Arrange
        User user = User.builder()
                .id(11L)
                .email("otpfixed01@example.com")
                .roles(Collections.singleton(userRole))
                .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));

        // Act
        LoginResponse.UserInfoDto response = authService.getUserInfo(11L);

        // Assert
        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals("otpfixed01@example.com", response.getEmail());
        assertTrue(response.getRoles().contains("USER"));
    }

    @Test
    public void testGetUserInfo_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        LoginVerificationException exception = assertThrows(LoginVerificationException.class,
                () -> authService.getUserInfo(99L));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void testPrintHashes() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder enc = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        System.out.println("BCRYPT_HASH_ADMIN: " + enc.encode("Admin@123456"));
        System.out.println("BCRYPT_HASH_CITIZEN: " + enc.encode("Citizen@123456"));
    }
}

