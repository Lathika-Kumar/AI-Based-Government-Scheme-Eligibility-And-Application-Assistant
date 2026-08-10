package com.schemebridge.authservice.service;

import com.schemebridge.authservice.entity.LoginAuditEntity;
import com.schemebridge.authservice.enums.LoginStatus;
import com.schemebridge.authservice.repository.LoginAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    public LoginAuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    @Transactional
    public void recordLoginAttempt(String userId, String email, LoginStatus status, String clientIp, String userAgent, String deviceInfo, String failureReason) {
        LoginAuditEntity audit = new LoginAuditEntity();
        audit.setAuditId(UUID.randomUUID().toString());
        audit.setUserId(userId);
        audit.setEmail(email);
        audit.setLoginStatus(status);
        audit.setClientIp(clientIp);
        audit.setUserAgent(userAgent);
        audit.setDeviceInfo(deviceInfo);
        audit.setFailureReason(failureReason);
        loginAuditRepository.save(audit);
    }
}
