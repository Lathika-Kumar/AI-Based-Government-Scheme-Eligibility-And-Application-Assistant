package com.schemebridge.authservice.service;

import com.schemebridge.authservice.entity.OtpVerificationEntity;
import com.schemebridge.authservice.enums.OtpType;
import com.schemebridge.authservice.exception.OtpException;
import com.schemebridge.authservice.repository.OtpVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final BrevoEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository, BrevoEmailService emailService, PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateAndSendOtp(String email, OtpType otpType) {
        String plainOtp = String.format("%06d", secureRandom.nextInt(1000000));
        org.slf4j.LoggerFactory.getLogger(OtpService.class).info("Generated OTP code for {}: {}", email, plainOtp);
        String otpHash = passwordEncoder.encode(plainOtp);

        OtpVerificationEntity otpEntity = new OtpVerificationEntity();
        otpEntity.setOtpId(UUID.randomUUID().toString());
        otpEntity.setEmail(email);
        otpEntity.setOtpHash(otpHash);
        otpEntity.setOtpType(otpType);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        otpEntity.setVerified(false);
        otpEntity.setAttempts(0);

        otpRepository.save(otpEntity);

        String subject = "SchemeBridge - Your OTP Verification Code";
        if (otpType == OtpType.PASSWORD_RESET) {
            subject = "SchemeBridge - Password Reset Code";
        }
        emailService.sendOtpEmail(email, plainOtp, subject);

        return plainOtp;
    }

    @Transactional
    public void verifyOtp(String email, String plainOtp, OtpType otpType) {
        Optional<OtpVerificationEntity> otpOpt = otpRepository.findTopByEmailAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(email, otpType);

        if (otpOpt.isEmpty()) {
            throw new OtpException("No active OTP verification code found for " + email);
        }

        OtpVerificationEntity otp = otpOpt.get();

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OtpException("OTP verification code has expired. Please request a new code");
        }

        if (otp.getAttempts() >= 3) {
            throw new OtpException("Maximum OTP verification attempts exceeded. Please request a new code");
        }

        if (!passwordEncoder.matches(plainOtp, otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new OtpException("Invalid OTP verification code");
        }

        otp.setVerified(true);
        otp.setStatus("VERIFIED");
        otpRepository.save(otp);
    }
}
