package com.schemebridge.authservice.repository;

import com.schemebridge.authservice.entity.OtpVerificationEntity;
import com.schemebridge.authservice.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerificationEntity, String> {
    Optional<OtpVerificationEntity> findTopByEmailAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(String email, OtpType otpType);
}
