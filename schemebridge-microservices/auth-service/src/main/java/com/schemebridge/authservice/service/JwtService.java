package com.schemebridge.authservice.service;

import com.schemebridge.authservice.entity.RefreshTokenEntity;
import com.schemebridge.authservice.exception.InvalidTokenException;
import com.schemebridge.authservice.repository.RefreshTokenRepository;
import com.schemebridge.authservice.security.JwtTokenProvider;
import com.schemebridge.authservice.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtService(JwtTokenProvider tokenProvider, RefreshTokenRepository refreshTokenRepository) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createAccessToken(UserPrincipal userPrincipal) {
        return tokenProvider.generateAccessToken(userPrincipal);
    }

    @Transactional
    public String createRefreshToken(String userId, String deviceName, String deviceId, String ipAddress) {
        String refreshTokenValue = tokenProvider.generateRefreshToken(userId);

        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setTokenId(UUID.randomUUID().toString());
        refreshTokenEntity.setUserId(userId);
        refreshTokenEntity.setToken(refreshTokenValue);
        refreshTokenEntity.setDeviceName(deviceName);
        refreshTokenEntity.setDeviceId(deviceId);
        refreshTokenEntity.setIpAddress(ipAddress);
        refreshTokenEntity.setLastUsedAt(LocalDateTime.now());
        refreshTokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenEntity.setRevoked(false);

        refreshTokenRepository.save(refreshTokenEntity);
        return refreshTokenValue;
    }

    @Transactional
    public RefreshTokenEntity verifyAndRotateRefreshToken(String refreshTokenValue) {
        if (!tokenProvider.validateToken(refreshTokenValue)) {
            throw new InvalidTokenException("Invalid or expired refresh token signature");
        }

        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidTokenException("Refresh token does not exist in registry"));

        if (tokenEntity.isRevoked()) {
            revokeAllUserTokens(tokenEntity.getUserId());
            throw new InvalidTokenException("Revoked refresh token presented. Security alert triggered - session terminated");
        }

        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenEntity.setRevoked(true);
            refreshTokenRepository.save(tokenEntity);
            throw new InvalidTokenException("Refresh token has expired. Please log in again");
        }

        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        return tokenEntity;
    }

    @Transactional
    public void revokeToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setRevoked(true);
            token.setStatus("REVOKED");
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        List<RefreshTokenEntity> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshTokenEntity token : activeTokens) {
            token.setRevoked(true);
            token.setStatus("REVOKED");
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    public String getUserIdFromToken(String token) {
        return tokenProvider.getUserIdFromToken(token);
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    public long getExpirationMs() {
        return tokenProvider.getExpirationMs();
    }
}
