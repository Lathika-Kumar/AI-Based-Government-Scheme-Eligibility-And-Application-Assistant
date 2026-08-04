package com.schemebridge.repository;

import com.schemebridge.entity.OtpToken;
import com.schemebridge.enums.VerificationMethod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findTopByUserIdAndTypeAndUsedFalseOrderByLastSentAtDesc(String userId, VerificationMethod type);
    List<OtpToken> findByUserId(String userId);
}
