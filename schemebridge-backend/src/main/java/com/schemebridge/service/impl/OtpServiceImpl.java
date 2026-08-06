package com.schemebridge.service.impl;

import com.schemebridge.dto.OtpResponse;
import com.schemebridge.dto.SendOtpRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.dto.VerifyOtpRequest;
import com.schemebridge.entity.CitizenProfile;
import com.schemebridge.entity.OtpToken;
import com.schemebridge.entity.User;
import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.enums.VerificationMethod;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.UserMapper;
import com.schemebridge.repository.CitizenProfileRepository;
import com.schemebridge.repository.OtpTokenRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.OtpService;
import com.schemebridge.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private static final int RESEND_COOLDOWN_SECONDS = 30; // 30 seconds
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int MAX_RESEND_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final NotificationService notificationService;
    private final com.schemebridge.service.email.EmailProvider emailProvider;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public OtpResponse sendEmailOtp(SendOtpRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim();
        log.info("Initiating Email OTP generation for email: {}", normalizedEmail);
        User user = userRepository.findByEmail(normalizedEmail)
            .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedEmail));

        user.setVerificationMethod(VerificationMethod.EMAIL);
        userRepository.save(user);

        return generateAndSendOtp(user, VerificationMethod.EMAIL, user.getEmail());
    }

    // Mobile/SMS OTP flow removed.

    @Override
    public UserDto verifyOtp(VerifyOtpRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim();
        log.info("Processing OTP verification for user email: {} via {}", normalizedEmail, request.getVerificationMethod());
        User user = userRepository.findByEmail(normalizedEmail)
            .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + normalizedEmail));

        Optional<OtpToken> tokenOpt = otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByLastSentAtDesc(
                user.getId(), request.getVerificationMethod()
        );

        if (tokenOpt.isEmpty()) {
            throw new BadRequestException("No active OTP request found. Please request a new OTP.");
        }

        OtpToken token = tokenOpt.get();

        // 1. Check if token was already used
        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new BadRequestException("This OTP has already been used. Please request a new OTP.");
        }

        // 2. Check if token expired (> 5 minutes)
        if (Instant.now().isAfter(token.getExpiryTime())) {
            token.setUsed(true);
            otpTokenRepository.save(token);
            throw new BadRequestException("OTP expired. Please request a new OTP.");
        }

        // 3. Check verification attempt limits (max 5)
        if (token.getVerificationAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            token.setUsed(true);
            otpTokenRepository.save(token);
            throw new BadRequestException("Maximum verification attempts reached. Please request a new OTP.");
        }

        // Increment attempt counter
        token.setVerificationAttempts(token.getVerificationAttempts() + 1);

        // 4. Validate OTP code
        if (!token.getCode().equals(request.getOtp())) {
            int remaining = MAX_VERIFICATION_ATTEMPTS - token.getVerificationAttempts();
            if (remaining <= 0) {
                token.setUsed(true);
                otpTokenRepository.save(token);
                throw new BadRequestException("Maximum verification attempts reached. Please request a new OTP.");
            }
            otpTokenRepository.save(token);
            throw new BadRequestException("Invalid OTP code. " + remaining + " attempts remaining.");
        }

        // Mark OTP as used
        token.setUsed(true);
        otpTokenRepository.save(token);

        // Update User account state to ACTIVE (Email-only verification supported)
        if (request.getVerificationMethod() != VerificationMethod.EMAIL) {
            throw new BadRequestException("Mobile/SMS verification has been removed. Only EMAIL verification is supported.");
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setVerificationMethod(VerificationMethod.EMAIL);
        user.setEmailVerified(true);

        User savedUser = userRepository.save(user);

        // Create CitizenProfile ONLY AFTER successful OTP verification for CITIZEN role
        if (savedUser.getRoles().contains(RoleEnum.CITIZEN)) {
            if (citizenProfileRepository.findByUserId(savedUser.getId()).isEmpty()) {
                CitizenProfile profile = CitizenProfile.builder()
                        .userId(savedUser.getId())
                        .build();
                citizenProfileRepository.save(profile);
                log.info("CitizenProfile document instantiated for verified user ID: {}", savedUser.getId());
            }
        }

        log.info("User {} successfully verified OTP and account status is now ACTIVE", savedUser.getEmail());
        return userMapper.toUserDto(savedUser);
    }

    private OtpResponse generateAndSendOtp(User user, VerificationMethod method, String recipient) {
        Optional<OtpToken> existingOpt = otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByLastSentAtDesc(
                user.getId(), method
        );

        int currentResendCount = 0;
        if (existingOpt.isPresent()) {
            OtpToken existing = existingOpt.get();

            // Check 30-second resend cooldown
            if (existing.getLastSentAt() != null) {
                long secondsSinceLastSent = Duration.between(existing.getLastSentAt(), Instant.now()).getSeconds();
                if (secondsSinceLastSent < RESEND_COOLDOWN_SECONDS) {
                    long waitTime = RESEND_COOLDOWN_SECONDS - secondsSinceLastSent;
                    throw new BadRequestException("Resend available in " + waitTime + " seconds");
                }
            }

            // Check maximum resend limit (max 3 resends)
            if (existing.getResendCount() >= MAX_RESEND_ATTEMPTS) {
                throw new BadRequestException("Maximum resend attempts reached. Please wait or try another verification method.");
            }

            currentResendCount = existing.getResendCount() + 1;
            // Invalidate existing token
            existing.setUsed(true);
            otpTokenRepository.save(existing);
        }

        // Generate 6-digit numeric OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(OTP_EXPIRY_SECONDS);

        OtpToken newToken = OtpToken.builder()
                .userId(user.getId())
                .code(otpCode)
                .type(method)
                .expiryTime(expiry)
                .used(false)
                .verificationAttempts(0)
                .resendCount(currentResendCount)
                .lastSentAt(now)
                .build();

        otpTokenRepository.save(newToken);

        // Only Email OTP is supported now.
        notificationService.sendEmailOtp(recipient, otpCode);

        OtpResponse.OtpResponseBuilder responseBuilder = OtpResponse.builder()
                .message("OTP sent successfully to " + recipient)
                .method(method)
                .recipient(recipient)
                .expirySeconds(OTP_EXPIRY_SECONDS)
                .remainingVerificationAttempts(MAX_VERIFICATION_ATTEMPTS)
                .resendCount(currentResendCount);

        // Include simulatedOtp only when the active provider for the requested method is simulated.
        if (method == VerificationMethod.EMAIL) {
            if (emailProvider instanceof com.schemebridge.service.email.SimulatedEmailProvider) {
                responseBuilder.simulatedOtp(otpCode);
            }
        }

        return responseBuilder.build();
    }
}
