package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.Application;
import com.schemebridge.scheme.document.ApplicationStatus;
import com.schemebridge.scheme.document.Scheme;
import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.dto.response.PagedApplicationResponse;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApplicationAdminVisibilityRegressionTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    private static final String TEST_CITIZEN_ID = "reg-test-citizen-999";
    private static final String TEST_SCHEME_CODE = "REG-VIS-TEST-01";
    private String createdApplicationId;

    @BeforeEach
    public void setUp() {
        // Ensure test scheme exists
        schemeRepository.findBySchemeCode(TEST_SCHEME_CODE).ifPresent(schemeRepository::delete);
        Scheme scheme = Scheme.builder()
                .schemeCode(TEST_SCHEME_CODE)
                .title(com.schemebridge.scheme.document.MultilingualText.builder().english("Admin Visibility Regression Test Scheme").build())
                .status(SchemeStatus.ACTIVE)
                .build();
        schemeRepository.save(scheme);
    }

    @AfterEach
    public void tearDown() {
        if (createdApplicationId != null) {
            applicationDocumentRepository.findAllByApplicationId(createdApplicationId)
                    .forEach(applicationDocumentRepository::delete);
            applicationRepository.findById(createdApplicationId).ifPresent(applicationRepository::delete);
        }
        schemeRepository.findBySchemeCode(TEST_SCHEME_CODE).ifPresent(schemeRepository::delete);
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Newly submitted application must be persisted in MongoDB, have non-null createdAt, and appear at top of Admin queue")
    public void testNewlyCreatedApplication_ReflectsInAdminQueueAtTop() {
        // 1. Authenticate as legitimate citizen
        authenticate(TEST_CITIZEN_ID, "ROLE_USER");

        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(32)
                .state("National")
                .annualIncome(150000.0)
                .gender("MALE")
                .occupation("SERVICE")
                .socialCategory("GENERAL")
                .build();

        CreateApplicationRequest request = CreateApplicationRequest.builder()
                .schemeCode(TEST_SCHEME_CODE)
                .profile(profile)
                .build();

        // 2. Citizen creates application through the real service & persistence
        ApplicationResponse createdResponse = applicationService.createApplication(request, TEST_CITIZEN_ID);
        assertNotNull(createdResponse);
        assertNotNull(createdResponse.getId());
        createdApplicationId = createdResponse.getId();

        // 3. Database direct verification
        Optional<Application> mongoRecordOpt = applicationRepository.findById(createdApplicationId);
        assertTrue(mongoRecordOpt.isPresent(), "Application must be persisted in MongoDB");
        Application mongoRecord = mongoRecordOpt.get();
        assertEquals(TEST_CITIZEN_ID, mongoRecord.getUserId(), "Citizen identity must match");
        assertEquals(TEST_SCHEME_CODE, mongoRecord.getSchemeCode(), "Scheme code must match");
        assertNotNull(mongoRecord.getCreatedAt(), "Application createdAt MUST be populated and non-null");

        // 4. Authenticate as Admin
        authenticate("admin-officer-1", "ROLE_ADMIN");

        // 5. Admin queries application review queue with default sort (createdAt DESC)
        PagedApplicationResponse adminQueue = applicationReviewService.getApplicationsQueue(
                null, null, 0, 20, "createdAt", "DESC"
        );

        assertNotNull(adminQueue);
        assertNotNull(adminQueue.getContent());
        assertFalse(adminQueue.getContent().isEmpty(), "Admin queue must return applications");

        // 6. Assert the newly created application is returned at the very top (index 0)
        ApplicationResponse topApplication = adminQueue.getContent().get(0);
        assertEquals(createdApplicationId, topApplication.getId(),
                "Newly submitted application MUST appear at the top of the Admin queue, not buried by null-timestamp sorting");
        assertEquals(TEST_CITIZEN_ID, topApplication.getUserId());
        assertEquals(TEST_SCHEME_CODE, topApplication.getSchemeCode());
        assertNotNull(topApplication.getCreatedAt());

        // 7. Verify Admin can find it by schemeCode as well
        PagedApplicationResponse schemeFiltered = applicationReviewService.getApplicationsQueue(
                null, TEST_SCHEME_CODE, 0, 10, "createdAt", "DESC"
        );
        assertEquals(1, schemeFiltered.getContent().size());
        assertEquals(createdApplicationId, schemeFiltered.getContent().get(0).getId());
    }
}
