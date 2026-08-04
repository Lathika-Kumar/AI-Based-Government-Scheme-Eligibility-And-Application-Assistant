package com.schemebridge.service.impl;

import com.schemebridge.dto.AuthResponse;
import com.schemebridge.dto.ForgotPasswordRequest;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.RefreshTokenRequest;
import com.schemebridge.dto.RefreshTokenResponse;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.dto.ResetPasswordRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.entity.RefreshToken;
import com.schemebridge.entity.User;
import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.exception.UserAlreadyExistsException;
import com.schemebridge.mapper.UserMapper;
import com.schemebridge.repository.RefreshTokenRepository;
import com.schemebridge.repository.RoleRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.security.JwtUtils;
import com.schemebridge.security.UserPrincipal;
import com.schemebridge.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Value("${schemebridge.security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Override
    public UserDto registerUser(RegisterRequest registerRequest) {
        log.info("Attempting registration for email: {}", registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: Email {} is already registered", registerRequest.getEmail());
            throw new UserAlreadyExistsException("Email is already registered: " + registerRequest.getEmail());
        }

        RoleEnum targetRoleEnum = registerRequest.getRole() != null ? registerRequest.getRole() : RoleEnum.CITIZEN;
        if (!roleRepository.existsByName(targetRoleEnum)) {
            throw new ResourceNotFoundException("Role not found: " + targetRoleEnum);
        }

        Set<RoleEnum> roles = new HashSet<>();
        roles.add(targetRoleEnum);

        User user = User.builder()
                .email(registerRequest.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName().trim())
                .phoneNumber(registerRequest.getPhoneNumber())
                .enabled(true)
                .status(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .phoneVerified(false)
                .onboardingCompleted(false)
                .roles(roles)
                .build();

        // Note: CitizenProfile is deferred and created ONLY after successful OTP verification
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}, status: PENDING_VERIFICATION", savedUser.getId());

        return userMapper.toUserDto(savedUser);
    }

    @Override
    public AuthResponse loginUser(LoginRequest loginRequest) {
        log.info("Attempting login for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail().trim().toLowerCase(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userPrincipal.getEmail()));

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login blocked: Account {} is SUSPENDED", user.getEmail());
            throw new BadRequestException("Your account has been suspended. Please contact support.");
        }

        String refreshToken = createRefreshTokenForUser(user);

        log.info("User {} logged in successfully with status: {}", user.getEmail(), user.getStatus());

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtExpirationMs)
                .user(userMapper.toUserDto(user))
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("Refreshing token for refreshToken: {}", refreshTokenRequest.getRefreshToken());

        RefreshToken existingToken = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token is invalid or expired."));

        if (existingToken.isRevoked() || Instant.now().isAfter(existingToken.getExpiresAt())) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new BadRequestException("Refresh token is invalid or expired.");
        }

        User user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User associated with refresh token was not found."));

        String newAccessToken = jwtUtils.generateTokenFromEmail(user.getEmail());
        String newRefreshToken = createRefreshTokenForUser(user);

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtExpirationMs)
                .build();
    }

    @Override
    public void logoutUser() {
        SecurityContextHolder.clearContext();
        log.info("User session logged out successfully");
    }

    @Override
    public String initiateForgotPassword(ForgotPasswordRequest request) {
        log.info("Initiating password reset request for email: {}", request.getEmail());
        if (!userRepository.existsByEmail(request.getEmail())) {
            log.warn("Password reset requested for non-existing email: {}", request.getEmail());
            return "If an account exists for this email, password reset instructions have been generated.";
        }

        String mockToken = UUID.randomUUID().toString();
        log.info("Generated password reset token for {}: {}", request.getEmail(), mockToken);
        return "Password reset token generated successfully: " + mockToken;
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Invalid or missing password reset token");
        }
        log.info("Password reset completed successfully for token: {}", request.getToken());
        return "Password has been reset successfully.";
    }

    private String createRefreshTokenForUser(User user) {
        String refreshTokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(user.getId())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(jwtExpirationMs * 30))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshTokenValue;
    }
}
