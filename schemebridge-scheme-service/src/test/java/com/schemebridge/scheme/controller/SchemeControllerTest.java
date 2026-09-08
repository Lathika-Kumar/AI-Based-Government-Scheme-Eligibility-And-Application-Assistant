package com.schemebridge.scheme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.config.SecurityConfig;
import com.schemebridge.scheme.document.MultilingualText;
import com.schemebridge.scheme.document.SchemeCategoryRef;
import com.schemebridge.scheme.document.SchemeStatus;
import com.schemebridge.scheme.document.SourceMetadata;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.SchemeCreateRequest;
import com.schemebridge.scheme.dto.response.BulkEligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.EligibilityEvaluationResponse;
import com.schemebridge.scheme.dto.response.SchemeEvaluationSummary;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import com.schemebridge.scheme.security.JwtAuthenticationFilter;
import com.schemebridge.scheme.security.JwtTokenProvider;
import com.schemebridge.scheme.service.CategoryService;
import com.schemebridge.scheme.service.SchemeRecommendationService;
import com.schemebridge.scheme.service.SchemeSearchService;
import com.schemebridge.scheme.service.SchemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchemeController.class)
@Import(SecurityConfig.class)
public class SchemeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SchemeService schemeService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private SchemeSearchService schemeSearchService;

    @MockBean
    private SchemeRecommendationService schemeRecommendationService;

    @MockBean
    private com.schemebridge.scheme.service.AdminAuditService adminAuditService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.schemebridge.scheme.service.GridFsDocumentStorageService gridFsDocumentStorageService;

    @MockBean
    private com.schemebridge.scheme.service.PdfTextExtractionService pdfTextExtractionService;

    @MockBean
    private com.schemebridge.scheme.service.GeminiSchemeExtractionService geminiSchemeExtractionService;

    private SchemeResponse activeScheme;
    private SchemeResponse draftScheme;

    @BeforeEach
    public void setUp() {
        activeScheme = SchemeResponse.builder()
                .id("sch-active")
                .schemeCode("SCH001")
                .slug("active-scheme")
                .title(MultilingualText.builder().english("Active Title").tamil("Tamil Active").build())
                .description(MultilingualText.builder().english("Active Desc").tamil("Tamil Active Desc").build())
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .status(SchemeStatus.ACTIVE)
                .build();

        draftScheme = SchemeResponse.builder()
                .id("sch-draft")
                .schemeCode("SCH002")
                .slug("draft-scheme")
                .title(MultilingualText.builder().english("Draft Title").tamil("Tamil Draft").build())
                .description(MultilingualText.builder().english("Draft Desc").tamil("Tamil Draft Desc").build())
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .status(SchemeStatus.DRAFT)
                .build();
    }

    @Test
    public void testGetSchemes_PublicAccess_ReturnsActiveSchemes() throws Exception {
        // Arrange
        when(schemeService.getAllSchemes(eq(SchemeStatus.ACTIVE), any())).thenReturn(List.of(activeScheme));

        // Act & Assert
        mockMvc.perform(get("/api/schemes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("sch-active"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    public void testGetSchemeById_PublicAccess_ActiveScheme_ReturnsOk() throws Exception {
        // Arrange
        when(schemeService.getSchemeById("sch-active")).thenReturn(activeScheme);

        // Act & Assert
        mockMvc.perform(get("/api/schemes/sch-active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sch-active"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void testGetSchemeById_PublicAccess_DraftScheme_ReturnsNotFound() throws Exception {
        // Arrange
        when(schemeService.getSchemeById("sch-draft")).thenReturn(draftScheme);

        // Act & Assert
        mockMvc.perform(get("/api/schemes/sch-draft")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Non-privileged cannot view draft schemes
    }

    @Test
    @WithMockUser(roles = "SCHEME_MANAGER")
    public void testGetSchemeById_ManagerAccess_DraftScheme_ReturnsOk() throws Exception {
        // Arrange
        when(schemeService.getSchemeById("sch-draft")).thenReturn(draftScheme);

        // Act & Assert
        mockMvc.perform(get("/api/schemes/sch-draft")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sch-draft"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    public void testCreateScheme_Unauthenticated_ReturnsForbidden() throws Exception {
        // Arrange
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH003")
                .slug("new-scheme")
                .categoryCode("EDU")
                .title(MultilingualText.builder().english("New").tamil("New Ta").build())
                .description(MultilingualText.builder().english("Desc").tamil("Desc Ta").build())
                .source(SourceMetadata.builder().sourceType("OFFICIAL_CENTRAL_GOVERNMENT").verificationStatus("VERIFIED").build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testCreateScheme_UserRole_ReturnsForbidden() throws Exception {
        // Arrange
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH003")
                .slug("new-scheme")
                .categoryCode("EDU")
                .title(MultilingualText.builder().english("New").tamil("New Ta").build())
                .description(MultilingualText.builder().english("Desc").tamil("Desc Ta").build())
                .source(SourceMetadata.builder().sourceType("OFFICIAL_CENTRAL_GOVERNMENT").verificationStatus("VERIFIED").build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SCHEME_MANAGER")
    public void testCreateScheme_ManagerRole_ReturnsCreated() throws Exception {
        // Arrange
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH003")
                .slug("new-scheme")
                .categoryCode("EDU")
                .title(MultilingualText.builder().english("New").tamil("New Ta").build())
                .description(MultilingualText.builder().english("Desc").tamil("Desc Ta").build())
                .source(SourceMetadata.builder().sourceType("OFFICIAL_CENTRAL_GOVERNMENT").verificationStatus("VERIFIED").build())
                .build();

        SchemeResponse createdResponse = SchemeResponse.builder()
                .id("sch-created")
                .schemeCode("SCH003")
                .slug("new-scheme")
                .title(request.getTitle())
                .description(request.getDescription())
                .status(SchemeStatus.DRAFT)
                .build();

        when(schemeService.createScheme(any())).thenReturn(createdResponse);

        // Act & Assert
        mockMvc.perform(post("/api/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sch-created"))
                .andExpect(jsonPath("$.schemeCode").value("SCH003"));
    }

    @Test
    @WithMockUser(roles = "VERIFICATION_OFFICER")
    public void testCreateScheme_VerificationOfficerRole_ReturnsForbidden() throws Exception {
        // Arrange
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH003")
                .slug("new-scheme")
                .categoryCode("EDU")
                .title(MultilingualText.builder().english("New").tamil("New Ta").build())
                .description(MultilingualText.builder().english("Desc").tamil("Desc Ta").build())
                .source(SourceMetadata.builder().sourceType("OFFICIAL_CENTRAL_GOVERNMENT").verificationStatus("VERIFIED").build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateScheme_InvalidJwt_ReturnsForbidden() throws Exception {
        // Arrange
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH003")
                .slug("new-scheme")
                .categoryCode("EDU")
                .title(MultilingualText.builder().english("New").tamil("New Ta").build())
                .description(MultilingualText.builder().english("Desc").tamil("Desc Ta").build())
                .source(SourceMetadata.builder().sourceType("OFFICIAL_CENTRAL_GOVERNMENT").verificationStatus("VERIFIED").build())
                .build();

        when(jwtTokenProvider.validateToken("invalidToken")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/schemes")
                .header("Authorization", "Bearer invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEvaluateEligibilityById_Unauthenticated_ReturnsForbidden() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        mockMvc.perform(post("/api/schemes/sch-active/eligibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityById_UserRole_ReturnsOk() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();
        EligibilityEvaluationResponse response = EligibilityEvaluationResponse.builder()
                .schemeId("sch-active")
                .schemeCode("SCH001")
                .status(com.schemebridge.scheme.document.EvaluationStatus.ELIGIBLE)
                .build();

        when(schemeService.evaluateEligibilityById(eq("sch-active"), any())).thenReturn(response);

        mockMvc.perform(post("/api/schemes/sch-active/eligibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeId").value("sch-active"))
                .andExpect(jsonPath("$.status").value("ELIGIBLE"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityByCode_UserRole_ReturnsOk() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();
        EligibilityEvaluationResponse response = EligibilityEvaluationResponse.builder()
                .schemeId("sch-active")
                .schemeCode("SCH001")
                .status(com.schemebridge.scheme.document.EvaluationStatus.ELIGIBLE)
                .build();

        when(schemeService.evaluateEligibilityByCode(eq("SCH001"), any())).thenReturn(response);

        mockMvc.perform(post("/api/schemes/code/SCH001/eligibility/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemeId").value("sch-active"))
                .andExpect(jsonPath("$.status").value("ELIGIBLE"));
    }

    @Test
    public void testEvaluateEligibilityForAll_Unauthenticated_ReturnsForbidden() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        mockMvc.perform(post("/api/schemes/eligibility/evaluate-all")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_UserRole_ReturnsOk() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        SchemeEvaluationSummary summary = SchemeEvaluationSummary.builder()
                .schemeId("sch-active")
                .schemeCode("SCH001")
                .slug("active-scheme")
                .title(MultilingualText.builder().english("Active Title").build())
                .description(MultilingualText.builder().english("Active Desc").build())
                .matchedConditions(List.of("Age is >= 18"))
                .build();

        BulkEligibilityEvaluationResponse response = BulkEligibilityEvaluationResponse.builder()
                .eligible(List.of(summary))
                .build();

        when(schemeService.evaluateEligibilityForAll(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/schemes/eligibility/evaluate-all")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible[0].schemeCode").value("SCH001"))
                .andExpect(jsonPath("$.eligible[0].schemeId").value("sch-active"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testEvaluateEligibilityForAll_UserRole_RequestsDraftStatus_ForceActive() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(21).build();

        SchemeEvaluationSummary summary = SchemeEvaluationSummary.builder()
                .schemeId("sch-active")
                .schemeCode("SCH001")
                .slug("active-scheme")
                .title(MultilingualText.builder().english("Active Title").build())
                .description(MultilingualText.builder().english("Active Desc").build())
                .matchedConditions(List.of("Age is >= 18"))
                .build();

        BulkEligibilityEvaluationResponse response = BulkEligibilityEvaluationResponse.builder()
                .eligible(List.of(summary))
                .build();

        when(schemeService.evaluateEligibilityForAll(any(), eq(SchemeStatus.DRAFT))).thenReturn(response);

        mockMvc.perform(post("/api/schemes/eligibility/evaluate-all")
                .param("status", "DRAFT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profile))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible[0].schemeCode").value("SCH001"));
    }
}
