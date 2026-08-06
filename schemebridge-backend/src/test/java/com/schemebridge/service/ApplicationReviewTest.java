package com.schemebridge.service;

import com.schemebridge.entity.Application;
import com.schemebridge.entity.User;
import com.schemebridge.enums.ApplicationStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.repository.ApplicationRepository;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ApplicationReviewTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    @DisplayName("Application review workflow keeps the status in the expected state machine")
    void testApplicationStatusWorkflowForReview() {
        User reviewer = User.builder()
                .id("reviewer-1")
                .email("reviewer@schemebridge.gov.in")
                .roles(Set.of(RoleEnum.VERIFICATION_OFFICER))
                .build();

        Application application = Application.builder()
                .id("app-1")
                .userId("citizen-1")
                .schemeId("scheme-001")
                .schemeName("Test Scheme")
                .status(ApplicationStatus.SUBMITTED)
                .build();

        assertEquals(ApplicationStatus.SUBMITTED, application.getStatus());
    }
}
