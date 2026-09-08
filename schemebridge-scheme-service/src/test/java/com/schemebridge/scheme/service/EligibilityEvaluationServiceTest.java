package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.CitizenEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResult;
import com.schemebridge.scheme.dto.response.EligibilityStatus;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityEvaluationServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private CitizenProfileService citizenProfileService;

    @Mock
    private EligibilityEngineContract eligibilityEngine;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private EligibilityEvaluationService evaluationService;

    private CitizenProfile sampleProfile;
    private CitizenEligibilityProfile sampleEligibilityProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = CitizenProfile.builder()
                .userId("user-101")
                .displayName("Ramesh Kumar")
                .state("Tamil Nadu")
                .age(28)
                .annualIncome(180000.0)
                .socialCategory("OBC")
                .occupation("Farmer")
                .build();

        sampleEligibilityProfile = CitizenEligibilityProfile.builder()
                .state("Tamil Nadu")
                .age(28)
                .annualIncome(180000.0)
                .socialCategory("OBC")
                .occupation("Farmer")
                .build();
    }

    @Test
    @DisplayName("Evaluate All: Correctly partitions into eligible, notEligible, and insufficientData lists")
    void testEvaluateAll_Partitioning() {
        when(citizenProfileRepository.findByUserId("user-101")).thenReturn(Optional.of(sampleProfile));
        when(citizenProfileService.toCitizenEligibilityProfile(sampleProfile)).thenReturn(sampleEligibilityProfile);

        Scheme s1 = Scheme.builder().schemeCode("S1").status(SchemeStatus.ACTIVE).schemeLevel(SchemeLevel.CENTRAL).build();
        Scheme s2 = Scheme.builder().schemeCode("S2").status(SchemeStatus.ACTIVE).schemeLevel(SchemeLevel.STATE).stateOrUt("Tamil Nadu").build();
        Scheme s3 = Scheme.builder().schemeCode("S3").status(SchemeStatus.ACTIVE).schemeLevel(SchemeLevel.STATE).stateOrUt("Gujarat").build();

        when(mongoTemplate.find(any(Query.class), eq(Scheme.class))).thenReturn(List.of(s1, s2, s3));

        when(eligibilityEngine.evaluate(sampleEligibilityProfile, s1)).thenReturn(
                EligibilityEvaluationResult.builder().schemeCode("S1").status(EligibilityStatus.ELIGIBLE).build()
        );
        when(eligibilityEngine.evaluate(sampleEligibilityProfile, s2)).thenReturn(
                EligibilityEvaluationResult.builder().schemeCode("S2").status(EligibilityStatus.INSUFFICIENT_DATA).build()
        );
        when(eligibilityEngine.evaluate(sampleEligibilityProfile, s3)).thenReturn(
                EligibilityEvaluationResult.builder().schemeCode("S3").status(EligibilityStatus.NOT_ELIGIBLE).build()
        );

        CitizenEligibilityEvaluationResponse resp = evaluationService.evaluateCitizenAgainstAllSchemes("user-101");

        assertNotNull(resp);
        assertEquals("user-101", resp.getUserId());
        assertEquals("Tamil Nadu", resp.getCitizenState());
        assertEquals(3, resp.getSummary().getTotalEvaluated());
        assertEquals(1, resp.getSummary().getEligibleCount());
        assertEquals(1, resp.getSummary().getInsufficientDataCount());
        assertEquals(1, resp.getSummary().getNotEligibleCount());

        assertEquals(1, resp.getEligibleSchemes().size());
        assertEquals("S1", resp.getEligibleSchemes().get(0).getSchemeCode());

        assertEquals(1, resp.getInsufficientDataSchemes().size());
        assertEquals("S2", resp.getInsufficientDataSchemes().get(0).getSchemeCode());

        assertEquals(1, resp.getNotEligibleSchemes().size());
        assertEquals("S3", resp.getNotEligibleSchemes().get(0).getSchemeCode());
    }
}
