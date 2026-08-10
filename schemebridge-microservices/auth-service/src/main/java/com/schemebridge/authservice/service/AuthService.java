package com.schemebridge.authservice.service;

import com.schemebridge.authservice.dto.request.*;
import com.schemebridge.authservice.dto.response.AuthResponse;
import com.schemebridge.authservice.dto.response.TokenResponse;
import com.schemebridge.authservice.dto.response.UserSummaryResponse;
import com.schemebridge.authservice.entity.PermissionEntity;
import com.schemebridge.authservice.entity.RoleEntity;
import com.schemebridge.authservice.entity.UserEntity;
import com.schemebridge.authservice.enums.*;
import com.schemebridge.authservice.event.AccountLockedEvent;
import com.schemebridge.authservice.event.PasswordResetEvent;
import com.schemebridge.authservice.event.UserRegisteredEvent;
import com.schemebridge.authservice.exception.AccountLockedException;
import com.schemebridge.authservice.exception.AuthException;
import com.schemebridge.authservice.exception.UserAlreadyExistsException;
import com.schemebridge.authservice.repository.RoleRepository;
import com.schemebridge.authservice.repository.UserRepository;
import com.schemebridge.authservice.security.UserPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final LoginAuditService loginAuditService;
    private final SecurityEventService securityEventService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            LoginAuditService loginAuditService,
            SecurityEventService securityEventService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.loginAuditService = loginAuditService;
        this.securityEventService = securityEventService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User account with email " + request.getEmail() + " already exists");
        }

        UserEntity user = new UserEntity();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);

        RoleEntity citizenRole = roleRepository.findByRoleName("ROLE_CITIZEN")
                .orElseGet(() -> roleRepository.save(new RoleEntity(1L, "ROLE_CITIZEN", "Standard Citizen Role")));

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(citizenRole);
        user.setRoles(roles);

        UserEntity savedUser = userRepository.save(user);

        // Dispatch initial verification OTP
        otpService.generateAndSendOtp(savedUser.getEmail(), OtpType.EMAIL_VERIFICATION);

        // Publish UserRegisteredEvent
        eventPublisher.publishEvent(new UserRegisteredEvent(savedUser.getUserId(), savedUser.getEmail()));
        securityEventService.recordSecurityEvent(savedUser.getUserId(), SecurityEventType.USER_REGISTERED, null, "User registered successfully");

        return buildUserSummary(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
        UserEntity user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            loginAuditService.recordLoginAttempt(null, request.getEmail(), LoginStatus.FAILED_USER_NOT_FOUND, clientIp, userAgent, request.getDeviceName(), "User not found");
            throw new AuthException("Invalid credentials");
        }

        // Account Lock Inspection
        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
                // Lock Auto-expired
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
                log.info("Account lock auto-expired for user: {}", user.getEmail());
            } else {
                loginAuditService.recordLoginAttempt(user.getUserId(), user.getEmail(), LoginStatus.FAILED_ACCOUNT_LOCKED, clientIp, userAgent, request.getDeviceName(), "Account locked");
                throw new AccountLockedException("Account is locked due to consecutive failed attempts. Please try again after 30 minutes or reset your password");
            }
        }

        // Password Verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int failed = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failed);
            user.setLastFailedLogin(LocalDateTime.now());

            if (failed >= 5) {
                user.setAccountLocked(true);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                userRepository.save(user);

                loginAuditService.recordLoginAttempt(user.getUserId(), user.getEmail(), LoginStatus.FAILED_ACCOUNT_LOCKED, clientIp, userAgent, request.getDeviceName(), "Failed password attempt 5 - account locked");
                eventPublisher.publishEvent(new AccountLockedEvent(user.getUserId(), user.getEmail(), "5 consecutive failed login attempts"));
                securityEventService.recordSecurityEvent(user.getUserId(), SecurityEventType.ACCOUNT_LOCKED, clientIp, "Account locked due to 5 failed login attempts");

                throw new AccountLockedException("Account has been locked due to 5 consecutive failed login attempts. Please try again after 30 minutes or reset your password");
            }

            userRepository.save(user);
            loginAuditService.recordLoginAttempt(user.getUserId(), user.getEmail(), LoginStatus.FAILED_INVALID_CREDENTIALS, clientIp, userAgent, request.getDeviceName(), "Invalid password attempt " + failed + "/5");
            throw new AuthException("Invalid credentials. Attempt " + failed + " of 5");
        }

        // Reset Failure Counters on Successful Password Check
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtService.createAccessToken(userPrincipal);
        String refreshToken = jwtService.createRefreshToken(user.getUserId(), request.getDeviceName(), request.getDeviceId(), clientIp);

        loginAuditService.recordLoginAttempt(user.getUserId(), user.getEmail(), LoginStatus.SUCCESS, clientIp, userAgent, request.getDeviceName(), "Login successful");

        UserSummaryResponse userSummary = buildUserSummary(user);
        return new AuthResponse(accessToken, refreshToken, jwtService.getExpirationMs(), userSummary);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        var oldTokenEntity = jwtService.verifyAndRotateRefreshToken(request.getRefreshToken());
        UserEntity user = userRepository.findById(oldTokenEntity.getUserId())
                .orElseThrow(() -> new AuthException("User not found for refresh token"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtService.createAccessToken(userPrincipal);
        String newRefreshToken = jwtService.createRefreshToken(user.getUserId(), oldTokenEntity.getDeviceName(), oldTokenEntity.getDeviceId(), oldTokenEntity.getIpAddress());

        return new TokenResponse(newAccessToken, newRefreshToken, jwtService.getExpirationMs());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        jwtService.revokeToken(request.getRefreshToken());
    }

    @Transactional
    public void sendEmailOtp(SendOtpRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User account with email " + request.getEmail() + " does not exist"));
        otpService.generateAndSendOtp(request.getEmail(), OtpType.EMAIL_VERIFICATION);
    }

    @Transactional
    public void verifyEmailOtp(VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpType.EMAIL_VERIFICATION);
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User account not found"));
        user.setEmailVerified(true);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void sendPhoneOtp(SendOtpRequest request) {
        otpService.generateAndSendOtp(request.getEmail(), OtpType.PHONE_VERIFICATION);
    }

    @Transactional
    public void verifyPhoneOtp(VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpType.PHONE_VERIFICATION);
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User account not found"));
        user.setPhoneVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User account with email " + request.getEmail() + " does not exist"));
        otpService.generateAndSendOtp(request.getEmail(), OtpType.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode(), OtpType.PASSWORD_RESET);

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("User account not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        jwtService.revokeAllUserTokens(user.getUserId());
        eventPublisher.publishEvent(new PasswordResetEvent(user.getUserId(), user.getEmail()));
        securityEventService.recordSecurityEvent(user.getUserId(), SecurityEventType.PASSWORD_CHANGED, null, "Password reset via OTP verification");
    }

    public UserSummaryResponse validateToken(String token) {
        if (!jwtService.validateToken(token)) {
            throw new AuthException("Invalid or expired access token signature");
        }
        String userId = jwtService.getUserIdFromToken(token);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User identity not found"));
        return buildUserSummary(user);
    }

    private UserSummaryResponse buildUserSummary(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getRoleName)
                .collect(Collectors.toSet());

        Set<String> permissions = new HashSet<>();
        for (RoleEntity role : user.getRoles()) {
            for (PermissionEntity p : role.getPermissions()) {
                permissions.add(p.getPermissionName());
            }
        }

        return new UserSummaryResponse(
                user.getUserId(),
                user.getEmail(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                roleNames,
                permissions
        );
    }
}
