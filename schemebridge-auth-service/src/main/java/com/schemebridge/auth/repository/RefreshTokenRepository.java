package com.schemebridge.auth.repository;

import com.schemebridge.auth.entity.RefreshToken;
import com.schemebridge.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke ALL active (non-revoked) refresh tokens for a user.
     * Called after a successful password reset to invalidate all existing sessions.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    int revokeAllActiveTokensForUser(@Param("user") User user);
}
