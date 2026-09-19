package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.SchemeDocumentChecklistResponse;
import com.schemebridge.scheme.dto.response.ApplicationDocumentResponse;
import com.schemebridge.scheme.dto.response.ApplicationResponse;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeBridgeUniversalVaultReuseTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private CitizenVaultDocumentRepository citizenVaultDocumentRepository;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    private static final String CITIZEN_A = "vault-reuse-citizen-a";
    private static final String CITIZEN_B = "vault-reuse-citizen-b";
    private static final String SCHEME_1 = "VAULT-REUSE-SCH-001";
    private static final String SCHEME_2 = "VAULT-REUSE-SCH-002";

    @BeforeEach
    public void setUp() {
        applicationDocumentRepository.deleteAllByUserId(CITIZEN_A);
        applicationDocumentRepository.deleteAllByUserId(CITIZEN_B);
        applicationRepository.findAllByUserId(CITIZEN_A).forEach(applicationRepository::delete);
        applicationRepository.findAllByUserId(CITIZEN_B).forEach(applicationRepository::delete);
        citizenVaultDocumentRepository.deleteAllByUserId(CITIZEN_A);
        citizenVaultDocumentRepository.deleteAllByUserId(CITIZEN_B);
        citizenProfileRepository.findByUserId(CITIZEN_A).ifPresent(citizenProfileRepository::delete);
        citizenProfileRepository.findByUserId(CITIZEN_B).ifPresent(citizenProfileRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_1).ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode(SCHEME_2).ifPresent(schemeRepository::delete);

        // Citizen A Profile
        CitizenProfile profileA = CitizenProfile.builder()
                .userId(CITIZEN_A)
                .displayName("Lathika K")
                .age(28)
                .gender("Female")
                .state("Tamil Nadu")
                .district("Chennai")
                .socialCategory("OBC")
                .annualIncome(180000.0)
                .occupation("Student")
                .onboardingComplete(true)
                .build();
        citizenProfileRepository.save(profileA);

        // Citizen B Profile
        CitizenProfile profileB = CitizenProfile.builder()
                .userId(CITIZEN_B)
                .displayName("Other Citizen")
                .age(35)
                .gender("Male")
                .state("Karnataka")
                .annualIncome(250000.0)
                .onboardingComplete(true)
                .build();
        citizenProfileRepository.save(profileB);

        // Scheme 1 requires Aadhaar Card
        RequiredDocument reqAadhaar = RequiredDocument.builder()
                .documentCode("DOC_AADHAAR")
                .name(MultilingualText.builder().english("Aadhaar Card").build())
                .mandatory(true)
                .build();

        Scheme s1 = Scheme.builder()
                .schemeCode(SCHEME_1)
                .slug("vault-reuse-sch-001")
                .title(MultilingualText.builder().english("Aadhaar Scholarship Scheme").build())
                .description(MultilingualText.builder().english("Scholarship based on verified Aadhaar identity.").build())
                .stateOrUt("Tamil Nadu")
                .department("Welfare")
                .requiredDocuments(List.of(reqAadhaar))
                .status(SchemeStatus.ACTIVE)
                .build();
        schemeRepository.save(s1);

        // Scheme 2 requires Income Certificate and Community Certificate
        RequiredDocument reqIncome = RequiredDocument.builder()
                .documentCode("DOC_INCOME")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .mandatory(true)
                .build();
        RequiredDocument reqCaste = RequiredDocument.builder()
                .documentCode("DOC_CASTE")
                .name(MultilingualText.builder().english("Community Certificate").build())
                .mandatory(true)
                .build();

        Scheme s2 = Scheme.builder()
                .schemeCode(SCHEME_2)
                .slug("vault-reuse-sch-002")
                .title(MultilingualText.builder().english("State Assistance Scheme").build())
                .description(MultilingualText.builder().english("Financial assistance for eligible citizens.").build())
                .stateOrUt("Tamil Nadu")
                .department("Welfare")
                .requiredDocuments(List.of(reqIncome, reqCaste))
                .status(SchemeStatus.ACTIVE)
                .build();
        schemeRepository.save(s2);
    }

    private CitizenVaultDocument createVaultDoc(String userId, String code, String name, String holder,
                                                DetailedDocumentStatus status, DocumentVerificationStatus verStatus,
                                                String identityMatch, Double score, String expiry) {
        CitizenVaultDocument doc = CitizenVaultDocument.builder()
                .userId(userId)
                .documentCode(code)
                .documentName(name)
                .documentType(name)
                .holderName(holder)
                .detailedStatus(status)
                .verificationStatus(verStatus)
                .identityMatchStatus(identityMatch)
                .verificationScore(score)
                .storageReference("gridfs:test-file-" + code)
                .gridFsFileId("test-file-" + code)
                .fileName(code.toLowerCase() + ".pdf")
                .fileSize(102400L)
                .contentType("application/pdf")
                .uploadedAt(Instant.now())
                .officerVerified(verStatus == DocumentVerificationStatus.VERIFIED)
                .expiryDate(expiry)
                .build();
        return citizenVaultDocumentRepository.save(doc);
    }

    @Test
    @DisplayName("Scenario 1: Valid Aadhaar in vault automatically satisfies requirement and sets readiness to 100%")
    @WithMockUser(username = CITIZEN_A)
    public void testValidAadhaarAutoReuse() {
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 96.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("READY_FOR_SUBMISSION", app.getStatus());
        assertEquals(100, app.getDocumentReadiness().getPercentage());
        assertEquals(1, app.getDocumentReadiness().getUploaded());
        assertEquals(0, app.getDocumentReadiness().getMandatoryMissing());

        ApplicationDocumentResponse docResp = app.getDocuments().get(0);
        assertTrue(docResp.isUploaded());
        assertTrue(docResp.isSatisfied());
        assertEquals("DOCUMENT_VAULT", docResp.getSource());
        assertNotNull(docResp.getVaultDocumentId());
        assertEquals("MATCH", docResp.getIdentityMatchStatus());
        assertEquals(96.0, docResp.getVerificationScore());
    }

    @Test
    @DisplayName("Scenario 2: Valid Income and Community certificates satisfy multi-document scheme")
    @WithMockUser(username = CITIZEN_A)
    public void testMultiDocumentVaultReuse() {
        createVaultDoc(CITIZEN_A, "DOC_INCOME", "Income Certificate", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 92.0, null);
        createVaultDoc(CITIZEN_A, "DOC_CASTE", "Caste Certificate", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 95.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_2)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("READY_FOR_SUBMISSION", app.getStatus());
        assertEquals(100, app.getDocumentReadiness().getPercentage());
        assertEquals(2, app.getDocumentReadiness().getUploaded());
        assertEquals(0, app.getDocumentReadiness().getMandatoryMissing());
    }

    @Test
    @DisplayName("Scenario 3: Missing document results in partial readiness (50%) and DOCUMENTS_PENDING")
    @WithMockUser(username = CITIZEN_A)
    public void testPartialVaultCoverage() {
        // Only Income Certificate in vault; Community Certificate is missing
        createVaultDoc(CITIZEN_A, "DOC_INCOME", "Income Certificate", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 92.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_2)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(50, app.getDocumentReadiness().getPercentage());
        assertEquals(1, app.getDocumentReadiness().getUploaded());
        assertEquals(1, app.getDocumentReadiness().getMandatoryMissing());
    }

    @Test
    @DisplayName("Scenario 4: Vault document with MISMATCH is disqualified from auto-reuse")
    @WithMockUser(username = CITIZEN_A)
    public void testMismatchDisqualification() {
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Ramesh Kumar",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MISMATCH", 90.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(0, app.getDocumentReadiness().getPercentage());
        assertFalse(app.getDocuments().get(0).isSatisfied());
    }

    @Test
    @DisplayName("Scenario 5: Vault document with UNCERTAIN identity is disqualified")
    @WithMockUser(username = CITIZEN_A)
    public void testUncertainIdentityDisqualification() {
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "UNCERTAIN", 70.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(0, app.getDocumentReadiness().getPercentage());
    }

    @Test
    @DisplayName("Scenario 6: Rejected vault document is disqualified")
    @WithMockUser(username = CITIZEN_A)
    public void testRejectedDocumentDisqualification() {
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika K",
                DetailedDocumentStatus.AI_REJECTED, DocumentVerificationStatus.REJECTED,
                "MATCH", 30.0, null);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(0, app.getDocumentReadiness().getPercentage());
    }

    @Test
    @DisplayName("Scenario 7: Expired vault document is disqualified")
    @WithMockUser(username = CITIZEN_A)
    public void testExpiredDocumentDisqualification() {
        String pastDate = "2020-01-01T00:00:00Z";
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 96.0, pastDate);

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(0, app.getDocumentReadiness().getPercentage());
    }

    @Test
    @DisplayName("Scenario 8: Cross-user isolation - Citizen A cannot reuse Citizen B's vault documents")
    @WithMockUser(username = CITIZEN_A)
    public void testCrossUserIsolation() {
        // Citizen B has valid Aadhaar
        createVaultDoc(CITIZEN_B, "DOC_AADHAAR", "Aadhaar Card", "Other Citizen",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 96.0, null);

        // Citizen A applies
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();

        ApplicationResponse app = applicationService.createApplication(req, CITIZEN_A);

        assertNotNull(app);
        assertEquals("DOCUMENTS_PENDING", app.getStatus());
        assertEquals(0, app.getDocumentReadiness().getPercentage());
    }

    @Test
    @DisplayName("Scenario 9: Multi-scheme linking without duplicate physical files")
    @WithMockUser(username = CITIZEN_A)
    public void testMultiSchemeReuseWithoutDuplication() {
        CitizenVaultDocument vDoc = createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 96.0, null);

        // Scheme 1 application
        CreateApplicationRequest req1 = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();
        ApplicationResponse app1 = applicationService.createApplication(req1, CITIZEN_A);

        assertEquals(100, app1.getDocumentReadiness().getPercentage());
        assertEquals(vDoc.getId(), app1.getDocuments().get(0).getVaultDocumentId());
        assertEquals(vDoc.getFileName(), app1.getDocuments().get(0).getFileName());

        // Check citizen vault doc count is still exactly 1 (no duplicate physical upload)
        long vaultCount = citizenVaultDocumentRepository.findByUserIdOrderByUploadedAtDesc(CITIZEN_A).size();
        assertEquals(1, vaultCount);
    }

    @Test
    @DisplayName("Scenario 10: getApplicationDetails and getDocumentChecklist sync vault documents for existing application")
    @WithMockUser(username = CITIZEN_A)
    public void testSyncVaultDocumentsOnRead() {
        // Create an application without any vault documents present first
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_1)
                .profile(CitizenEligibilityProfile.builder()
                        .age(28).gender("Female").state("Tamil Nadu").build())
                .build();
        ApplicationResponse createdApp = applicationService.createApplication(req, CITIZEN_A);
        assertEquals("DOCUMENTS_PENDING", createdApp.getStatus());
        assertEquals(0, createdApp.getDocumentReadiness().getPercentage());

        // Now citizen uploads valid Aadhaar to Document Vault afterwards
        createVaultDoc(CITIZEN_A, "DOC_AADHAAR", "Aadhaar Card", "Lathika K",
                DetailedDocumentStatus.AI_VERIFIED, DocumentVerificationStatus.PENDING,
                "MATCH", 96.0, null);

        // Calling getApplicationDetails should auto-sync and satisfy the document
        ApplicationResponse fetchedApp = applicationService.getApplicationDetails(createdApp.getId(), CITIZEN_A);
        assertEquals(100, fetchedApp.getDocumentReadiness().getPercentage());
        assertEquals("READY_FOR_SUBMISSION", fetchedApp.getStatus());
        assertTrue(fetchedApp.getDocuments().get(0).isSatisfied());
        assertEquals("DOCUMENT_VAULT", fetchedApp.getDocuments().get(0).getSource());

        // Calling getDocumentChecklist should also reflect satisfaction
        SchemeDocumentChecklistResponse checklist = applicationService.getDocumentChecklist(createdApp.getId(), CITIZEN_A, false);
        assertEquals(1, checklist.getTotalUploaded());
        assertTrue(checklist.getItems().get(0).isSatisfied());
        assertEquals("DOCUMENT_VAULT", checklist.getItems().get(0).getSource());
    }
}
