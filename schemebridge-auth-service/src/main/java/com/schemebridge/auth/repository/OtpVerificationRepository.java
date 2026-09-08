package com.schemebridge.auth.repository;

import com.schemebridge.auth.entity.OtpPurpose;
import com.schemebridge.auth.entity.OtpVerification;
import com.schemebridge.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** Find the most-recently created OTP record for a user+purpose. */
    Optional<OtpVerification> findFirstByUserAndPurposeOrderByCreatedAtDesc(User user, OtpPurpose purpose);

    /**
     * Expire (invalidate) all unverified OTP records for a user+purpose by
     * setting expiresAt to a past time. Used before issuing a fresh OTP so
     * previous codes cannot be reused.
     */
    @Modifying
    @Query("UPDATE OtpVerification o SET o.expiresAt = :expiredAt " +
           "WHERE o.user = :user AND o.purpose = :purpose AND o.verifiedAt IS NULL AND o.expiresAt > :now")
    int expirePreviousOtps(@Param("user") User user,
                           @Param("purpose") OtpPurpose purpose,
                           @Param("now") LocalDateTime now,
                           @Param("expiredAt") LocalDateTime expiredAt);

    /** Find all unverified OTPs for a user+purpose (for rate-limit checks). */
    List<OtpVerification> findByUserAndPurposeAndVerifiedAtIsNull(User user, OtpPurpose purpose);
}
