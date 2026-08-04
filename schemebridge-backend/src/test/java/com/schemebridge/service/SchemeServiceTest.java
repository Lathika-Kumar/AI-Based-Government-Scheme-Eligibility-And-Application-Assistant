package com.schemebridge.service;

import com.schemebridge.dto.SchemeRequest;
import com.schemebridge.dto.SchemeResponse;
import com.schemebridge.entity.Scheme;
import com.schemebridge.enums.SchemeStatus;
import com.schemebridge.enums.SchemeType;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.SchemeMapper;
import com.schemebridge.repository.CitizenProfileRepository;
import com.schemebridge.repository.SchemeAuditLogRepository;
import com.schemebridge.repository.SchemeRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.impl.SchemeServiceImpl;
import com.schemebridge.util.EligibilityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemeServiceTest {

    private SchemeRepository schemeRepository;
    private SchemeAuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private CitizenProfileRepository citizenProfileRepository;
    private SchemeMapper schemeMapper;
    private EligibilityEngine eligibilityEngine;
    private SchemeService schemeService;

    @BeforeEach
    void setUp() {
        schemeRepository = mock(SchemeRepository.class);
        auditLogRepository = mock(SchemeAuditLogRepository.class);
        userRepository = mock(UserRepository.class);
        citizenProfileRepository = mock(CitizenProfileRepository.class);
        schemeMapper = new SchemeMapper();
        eligibilityEngine = mock(EligibilityEngine.class);

        schemeService = new SchemeServiceImpl(
                schemeRepository,
                auditLogRepository,
                userRepository,
                citizenProfileRepository,
                schemeMapper,
                eligibilityEngine,
                null
        );
    }

    @Test
    @DisplayName("createScheme persists scheme and creates audit log")
    void testCreateScheme() {
        SchemeRequest request = SchemeRequest.builder()
                .schemeName("Test Scheme")
                .schemeCode("TEST-001")
                .description("Description")
                .category("Education")
                .schemeType(SchemeType.CENTRAL)
                .build();

        Scheme savedScheme = Scheme.builder()
                .id("scheme-123")
                .schemeName("Test Scheme")
                .schemeCode("TEST-001")
                .category("Education")
                .schemeType(SchemeType.CENTRAL)
                .status(SchemeStatus.DRAFT)
                .build();

        when(schemeRepository.findBySchemeCode("TEST-001")).thenReturn(Optional.empty());
        when(schemeRepository.save(any(Scheme.class))).thenReturn(savedScheme);

        SchemeResponse response = schemeService.createScheme(request, "admin@schemebridge.gov.in");

        assertEquals("scheme-123", response.getId());
        assertEquals("Test Scheme", response.getSchemeName());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("getScheme throws ResourceNotFoundException if non-existent")
    void testGetSchemeNotFound() {
        when(schemeRepository.findByIdAndDeletedFalse("invalid-id")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> schemeService.getScheme("invalid-id"));
    }
}
