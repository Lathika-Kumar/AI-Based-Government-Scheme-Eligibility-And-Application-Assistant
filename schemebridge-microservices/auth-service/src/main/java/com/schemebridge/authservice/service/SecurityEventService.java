package com.schemebridge.authservice.service;

import com.schemebridge.authservice.entity.SecurityEventEntity;
import com.schemebridge.authservice.enums.SecurityEventType;
import com.schemebridge.authservice.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    public SecurityEventService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    @Transactional
    public void recordSecurityEvent(String userId, SecurityEventType eventType, String ipAddress, String details) {
        SecurityEventEntity event = new SecurityEventEntity();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setIpAddress(ipAddress);
        event.setDetails(details);
        securityEventRepository.save(event);
    }
}
