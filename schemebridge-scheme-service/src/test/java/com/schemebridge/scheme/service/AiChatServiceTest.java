package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.AiChatRequest;
import com.schemebridge.scheme.dto.response.AiChatResponse;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.FeedbackRepository;
import com.schemebridge.scheme.repository.GrievanceRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private com.schemebridge.scheme.repository.SchemeVerifiedDataRepository schemeVerifiedDataRepository;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private EligibilityEngine eligibilityEngine;

    @Mock
    private SchemeDocumentRequirementResolver documentRequirementResolver;

    @InjectMocks
    private AiChatService aiChatService;

    private CitizenProfile mockProfile;
    private com.schemebridge.scheme.dto.request.CitizenEligibilityProfile mockEligibilityDto;

    @BeforeEach
    void setUp() {
        mockProfile = CitizenProfile.builder()
                .userId("101")
                .displayName("Rajesh Patel")
                .age(35)
                .gender("Male")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("OBC")
                .state("Gujarat")
                .verifiedAttributes(Map.of("income", VerifiedAttribute.builder().verified(true).build()))
                .build();

        mockEligibilityDto = com.schemebridge.scheme.dto.request.CitizenEligibilityProfile.builder()
                .age(35)
                .gender("Male")
                .occupation("Farmer")
                .annualIncome(150000.0)
                .socialCategory("OBC")
                .state("Gujarat")
                .build();
    }
    @Test
    void testChatCitizen_ApplicationStatusIntent() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(mockProfile));
        when(citizenProfileService.toCitizenEligibilityProfile(mockProfile)).thenReturn(mockEligibilityDto);
        Application app = Application.builder()
                .applicationNumber("APP-2026-001")
                .schemeCode("PM-KISAN")
                .status(ApplicationStatus.UNDER_REVIEW)
                .build();
        when(applicationRepository.findAllByUserId("101")).thenReturn(List.of(app));

        AiChatRequest req = AiChatRequest.builder()
                .message("What is my application status?")
                .build();

        AiChatResponse response = aiChatService.chatCitizen(req, "101");

        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("APP-2026-001"));
        assertTrue(response.getResponse().contains("UNDER_REVIEW"));
        assertNotNull(response.getActionLink());
    }

    @Test
    void testChatCitizen_GrievanceIntent() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(mockProfile));
        when(citizenProfileService.toCitizenEligibilityProfile(mockProfile)).thenReturn(mockEligibilityDto);
        when(applicationRepository.findAllByUserId("101")).thenReturn(Collections.emptyList());

        AiChatRequest req = AiChatRequest.builder()
                .message("I have a complaint about payment delay")
                .build();

        AiChatResponse response = aiChatService.chatCitizen(req, "101");

        assertNotNull(response);
        assertTrue(response.getResponse().contains("Grievance"));
        assertEquals("/help", response.getActionLink().get("path"));
    }

    @Test
    void testChatCitizen_SchemeKeywordSearchGrounded() {
        when(citizenProfileRepository.findByUserId("101")).thenReturn(Optional.of(mockProfile));
        when(citizenProfileService.toCitizenEligibilityProfile(mockProfile)).thenReturn(mockEligibilityDto);
        when(applicationRepository.findAllByUserId("101")).thenReturn(Collections.emptyList());

        Scheme mockScheme = Scheme.builder()
                .schemeCode("PM-KISAN")
                .slug("pm-kisan-yojana")
                .title(MultilingualText.builder().english("Pradhan Mantri Kisan Samman Nidhi").build())
                .description(MultilingualText.builder().english("Income support scheme for farmers.").build())
                .ministry("Ministry of Agriculture")
                .schemeLevel(SchemeLevel.CENTRAL)
                .build();

        when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(List.of(mockScheme));
        when(eligibilityEngine.evaluate(any(), any())).thenReturn(
                com.schemebridge.scheme.dto.response.EligibilityEvaluationResult.builder()
                        .status(com.schemebridge.scheme.dto.response.EligibilityStatus.ELIGIBLE)
                        .passedConditions(List.of("Occupation is Farmer"))
                        .failedConditions(Collections.emptyList())
                        .build()
        );
        when(documentRequirementResolver.resolveRequirements(any())).thenReturn(Collections.emptyList());

        AiChatRequest req = AiChatRequest.builder()
                .message("Tell me about pm-kisan farming assistance")
                .build();

        AiChatResponse response = aiChatService.chatCitizen(req, "101");

        assertNotNull(response);
        assertTrue(response.getResponse().contains("Pradhan Mantri Kisan Samman Nidhi"));
        assertFalse(response.getRelatedSchemes().isEmpty());
        assertEquals("PM-KISAN", response.getRelatedSchemes().get(0).get("schemeCode"));
    }

    @Test
    void testChatAdmin_WorkloadIntent() {
        when(schemeRepository.count()).thenReturn(4734L);
        when(schemeVerifiedDataRepository.count()).thenReturn(4682L);
        when(applicationRepository.count()).thenReturn(50L);
        when(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)).thenReturn(10L);
        when(applicationRepository.countByStatus(ApplicationStatus.UNDER_REVIEW)).thenReturn(15L);
        when(applicationRepository.countByStatus(ApplicationStatus.APPROVED)).thenReturn(20L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(5L);

        AiChatRequest req = AiChatRequest.builder()
                .message("Summarize pending workload")
                .build();

        AiChatResponse response = aiChatService.chatAdmin(req, "admin-1", "ROLE_ADMIN");

        assertNotNull(response);
        assertTrue(response.getResponse().contains("Total Applications Received:** 50"));
        assertTrue(response.getResponse().contains("Pending Officer Review:** 25"));
        assertEquals("/admin/applications", response.getActionLink().get("path"));
    }

    @Test
    void testChatAdmin_GrievanceDeskIntent() {
        when(schemeRepository.count()).thenReturn(4734L);
        when(schemeVerifiedDataRepository.count()).thenReturn(4682L);
        when(grievanceRepository.count()).thenReturn(12L);
        when(grievanceRepository.countByStatus(GrievanceStatus.OPEN)).thenReturn(4L);
        when(grievanceRepository.countByStatus(GrievanceStatus.IN_PROGRESS)).thenReturn(3L);

        AiChatRequest req = AiChatRequest.builder()
                .message("Analyze unresolved grievances")
                .build();

        AiChatResponse response = aiChatService.chatAdmin(req, "admin-1", "ROLE_ADMIN");

        assertNotNull(response);
        assertTrue(response.getResponse().contains("Total Lodged Grievances:** 12"));
        assertTrue(response.getResponse().contains("Open / Unassigned: 4"));
        assertEquals("/admin/grievances", response.getActionLink().get("path"));
    }

    @Test
    void testChatAdmin_UnauthorizedRoleRejection() {
        AiChatRequest req = AiChatRequest.builder()
                .message("How many applications are pending?")
                .build();

        assertThrows(SecurityException.class, () -> {
            aiChatService.chatAdmin(req, "citizen-1", "ROLE_CITIZEN");
        });
    }
}
