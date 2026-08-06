package com.schemebridge.service;

import com.schemebridge.dto.ApplicationRequest;
import com.schemebridge.dto.ApplicationResponse;
import com.schemebridge.entity.User;
import com.schemebridge.enums.ApplicationStatus;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.repository.ApplicationRepository;
import com.schemebridge.repository.SavedSchemeRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SavedSchemeRepository savedSchemeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    @DisplayName("Duplicate submission is rejected for the same scheme and user")
    void testDuplicateSubmissionPrevention() {
        User user = User.builder().id("user-1").email("citizen@schemebridge.gov.in").build();
        ApplicationRequest request = ApplicationRequest.builder()
                .schemeId("scheme-001")
                .schemeName("Test Scheme")
                .ministry("Ministry")
                .documents(List.of("doc-1"))
                .build();

        when(userRepository.findByEmail("citizen@schemebridge.gov.in")).thenReturn(Optional.of(user));
        when(applicationRepository.findByUserIdAndSchemeId(user.getId(), request.getSchemeId())).thenReturn(Optional.of(new com.schemebridge.entity.Application()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> applicationService.submitApplication("citizen@schemebridge.gov.in", request));

        assertEquals("Duplicate application submission is not allowed for the same scheme.", ex.getMessage());
    }

    @Test
    @DisplayName("Save and unsave operations use the user-scoped saved scheme repository")
    void testSaveAndUnsaveScheme() {
        User user = User.builder().id("user-1").email("citizen@schemebridge.gov.in").build();
        when(userRepository.findByEmail("citizen@schemebridge.gov.in")).thenReturn(Optional.of(user));
        when(savedSchemeRepository.findByUserIdAndSchemeId(user.getId(), "scheme-001")).thenReturn(Optional.empty());

        applicationService.saveScheme("citizen@schemebridge.gov.in", "scheme-001");
        applicationService.unsaveScheme("citizen@schemebridge.gov.in", "scheme-001");
    }
}
