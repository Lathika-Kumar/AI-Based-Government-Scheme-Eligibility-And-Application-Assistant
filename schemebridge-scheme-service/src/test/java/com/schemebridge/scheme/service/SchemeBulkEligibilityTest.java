package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.SchemeEvaluationSummary;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeBulkEligibilityTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeService schemeService;

    private Scheme activeScheme;
    private Scheme draftScheme;

    @BeforeEach
    public void setUp() {
        schemeRepository.findBySchemeCode("BULK-TEST-ACTIVE").ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode("BULK-TEST-DRAFT").ifPresent(schemeRepository::delete);

        EligibilityCondition condition = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        RuleGroup eligibilityRules = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(condition))
                .build();

        activeScheme = Scheme.builder()
                .schemeCode("BULK-TEST-ACTIVE")
                .slug("bulk-test-active")
                .title(MultilingualText.builder().english("Active Bulk Scheme").build())
                .description(MultilingualText.builder().english("Active Desc").build())
                .eligibilityRules(eligibilityRules)
                .status(SchemeStatus.ACTIVE)
                .version(1)
                .build();

        draftScheme = Scheme.builder()
                .schemeCode("BULK-TEST-DRAFT")
                .slug("bulk-test-draft")
                .title(MultilingualText.builder().english("Draft Bulk Scheme").build())
                .description(MultilingualText.builder().english("Draft Desc").build())
                .eligibilityRules(eligibilityRules)
                .status(SchemeStatus.DRAFT)
                .version(1)
                .build();

        schemeRepository.save(activeScheme);
        schemeRepository.save(draftScheme);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_User_Eligible() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.ACTIVE);

        assertNotNull(response);
        boolean foundActive = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"));
        assertTrue(foundActive);

        boolean foundDraft = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-DRAFT"));
        assertFalse(foundDraft);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_User_Ineligible() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(17).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.ACTIVE);

        assertNotNull(response);
        boolean foundActive = response.getIneligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"));
        assertTrue(foundActive);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_User_Indeterminate() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.ACTIVE);

        assertNotNull(response);
        boolean foundActive = response.getIndeterminate().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"));
        assertTrue(foundActive);

        SchemeEvaluationSummary activeSummary = response.getIndeterminate().stream()
                .filter(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"))
                .findFirst().orElseThrow();
        assertTrue(activeSummary.getMissingInformation().contains("AGE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_UserRequestsDraft_ExposesActiveOnly() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.DRAFT);

        assertNotNull(response);
        boolean foundActive = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"));
        assertTrue(foundActive);

        boolean foundDraft = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-DRAFT"));
        assertFalse(foundDraft);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testEvaluateEligibilityForAll_AdminRequestsDraft_ExposesDraft() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.DRAFT);

        assertNotNull(response);
        boolean foundDraft = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-DRAFT"));
        assertTrue(foundDraft);

        boolean foundActive = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-ACTIVE"));
        assertFalse(foundActive);
    }

    @Test
    @WithMockUser(roles = "SCHEME_MANAGER")
    public void testEvaluateEligibilityForAll_ManagerRequestsDraft_ExposesDraft() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.DRAFT);

        assertNotNull(response);
        boolean foundDraft = response.getEligible().stream()
                .anyMatch(s -> s.getSchemeCode().equals("BULK-TEST-DRAFT"));
        assertTrue(foundDraft);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testEvaluateEligibilityForAll_EmptyResultHandling() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        BulkEligibilityEvaluationResponse response = schemeService.evaluateEligibilityForAll(profile, SchemeStatus.ARCHIVED);

        assertNotNull(response);
        assertNotNull(response.getEligible());
        assertNotNull(response.getIneligible());
        assertNotNull(response.getIndeterminate());
        assertTrue(response.getEligible().isEmpty());
    }
}
