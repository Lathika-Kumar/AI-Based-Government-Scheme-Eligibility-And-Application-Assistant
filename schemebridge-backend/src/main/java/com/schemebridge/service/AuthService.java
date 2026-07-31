package com.schemebridge.service;

import com.schemebridge.dto.AuthResponse;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.entity.User;
import com.schemebridge.enums.Role;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user authentication, registration, and password verification.
 * Timestamps (createdAt / updatedAt) are managed automatically by @EnableMongoAuditing.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    /**
     * Registers a new citizen user account.
     * Validates for duplicate email, BCrypt-encodes the password, assigns CITIZEN role, and persists.
     *
     * @param request the registration request payload
     * @return AuthResponse containing generated JWT token and user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Guard: reject duplicate email registrations
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration attempt failed: email '{}' already registered", normalizedEmail);
            throw new IllegalArgumentException("Email address is already in use");
        }

        // Encrypt password using BCrypt (cost factor 12, configured in WebSecurityConfig)
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Build User document — createdAt/updatedAt are populated automatically by MongoDB auditing
        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(encodedPassword)
                .mobile(request.getMobile() != null ? request.getMobile().trim() : null)
                .role(Role.CITIZEN)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered new citizen user with ID: {}", savedUser.getId());

        // Generate signed JWT token for immediate use
        String token = jwtUtils.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Authenticates an existing user and issues a JWT token.
     * Uses a generic "Invalid email or password" message on both email-not-found
     * and wrong-password scenarios to prevent user enumeration.
     *
     * @param request the login request payload
     * @return AuthResponse containing generated JWT token and user info
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Look up user — throw generic error to prevent user enumeration
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login attempt failed: user with email '{}' not found", normalizedEmail);
                    return new IllegalArgumentException("Invalid email or password");
                });

        // Verify raw password against stored BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login attempt failed: invalid password for email '{}'", normalizedEmail);
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User '{}' successfully authenticated with role '{}'", user.getEmail(), user.getRole());

        // Generate signed JWT token
        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
