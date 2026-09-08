package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.enums.RequirementProvenance;
import com.schemebridge.scheme.ocr.DocumentOcrProvider;
import com.schemebridge.scheme.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase12EndToEndWorkflowTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private ApplicationReviewRepository applicationReviewRepository;

    @Autowired
    private DocumentOcrResultRepository ocrResultRepository;

    @Autowired
    private CitizenProfileRepository citizenProfileRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private DocumentOcrService documentOcrService;

    @Autowired
    private SchemeDocumentRequirementResolver requirementResolver;

    private Scheme agriScheme;
    private static final String CITIZEN_ID = "citizen_sharma_65";
    private static final String SCHEME_CODE = "SCH-MAHA-AGRI-FARMER-2026";

    @BeforeEach
    void setUp() {
        applicationDocumentRepository.deleteAll();
        applicationRepository.deleteAll();
        ocrResultRepository.deleteAll();
        schemeRepository.findBySchemeCode(SCHEME_CODE).ifPresent(schemeRepository::delete);
        citizenProfileRepository.findByUserId(CITIZEN_ID).ifPresent(citizenProfileRepository::delete);

        // Scheme with AST Eligibility Rules (Income <= 300,000 and Category == OBC) and Financial Benefit
        EligibilityCondition incomeCondition = EligibilityCondition.builder()
                .field("ANNUAL_INCOME")
                .operator(RuleOperator.LESS_THAN_OR_EQUAL)
                .value("300000")
                .dataType("NUMBER")
                .required(true)
                .build();

        EligibilityCondition categoryCondition = EligibilityCondition.builder()
                .field("SOCIAL_CATEGORY")
                .operator(RuleOperator.EQUALS)
                .value("OBC")
                .dataType("STRING")
                .required(true)
                .build();

        RuleGroup rules = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(incomeCondition, categoryCondition))
                .build();

        agriScheme = Scheme.builder()
                .schemeCode(SCHEME_CODE)
                .slug("maha-agri-farmer-scheme")
                .title(MultilingualText.builder().english("Maharashtra Small Farmer Livelihood Scheme").build())
                .description(MultilingualText.builder().english("Income support scheme for OBC small farmers in Maharashtra").build())
                .status(SchemeStatus.ACTIVE)
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("Maharashtra")
                .category(SchemeCategoryRef.builder().name("Agriculture & Rural Development").build())
                .eligibilityRules(rules)
                .benefits(List.of(SchemeBenefit.builder().amountType("FINANCIAL").build()))
                .version(1)
                .build();

        schemeRepository.save(agriScheme);

        // Citizen Profile (Eligible: Income = 160000, Category = OBC)
        CitizenProfile profile = CitizenProfile.builder()
                .userId(CITIZEN_ID)
                .displayName("Aarav S. Sharma")
                .state("Maharashtra")
                .annualIncome(160000.0)
                .socialCategory("OBC")
                .onboardingComplete(true)
                .onboardingStatus("COMPLETE")
                .onboardingStep(3)
                .verifiedAttributes(new HashMap<>())
                .build();

        citizenProfileRepository.save(profile);
    }

    @AfterEach
    void tearDown() {
        applicationDocumentRepository.deleteAll();
        applicationRepository.deleteAll();
        ocrResultRepository.deleteAll();
        schemeRepository.findBySchemeCode(SCHEME_CODE).ifPresent(schemeRepository::delete);
        citizenProfileRepository.findByUserId(CITIZEN_ID).ifPresent(citizenProfileRepository::delete);
    }

    private byte[] createRealDigitalPdf(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                cs.newLineAtOffset(50, 700);
                for (String line : content.split("\n")) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("1. Document Requirement Resolver: Deterministically derives system requirements from rules without mutating catalog")
    void testDocumentRequirementResolver() {
        List<SchemeDocumentRequirementResolver.ResolvedRequirement> requirements =
                requirementResolver.resolveRequirements(agriScheme);

        assertNotNull(requirements);
        assertTrue(requirements.size() >= 4);

        // Aadhaar, Income Proof, Caste Certificate, Land Record, Bank Passbook, Domicile
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("AADHAAR") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("INCOME_PROOF") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("CASTE_CERT") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("DOMICILE_CERT") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("LAND_RECORD") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
        assertTrue(requirements.stream().anyMatch(r -> r.getDocumentCode().equals("BANK_PASSBOOK") && r.isMandatory() && r.getProvenance() == RequirementProvenance.SYSTEM_CONFIGURED));
    }

    @Test
    @DisplayName("2. Complete End-to-End Workflow: Apply -> View Checklist -> Upload -> OCR -> Admin Verify -> Re-eval -> Approve")
    void testCompleteEndToEndWorkflow_Success() throws IOException {
        // Step 1: Citizen creates application
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, CITIZEN_ID);

        assertNotNull(appRes);
        assertEquals("DOCUMENTS_PENDING", appRes.getStatus());

        // Step 2: Fetch Document Checklist API
        SchemeDocumentChecklistResponse checklist = applicationService.getDocumentChecklist(appRes.getId(), CITIZEN_ID, false);
        assertNotNull(checklist);
        assertTrue(checklist.getTotalRequired() >= 4);
        assertFalse(checklist.isCanSubmit());

        // Step 3: Citizen uploads all mandatory documents
        byte[] aadhaarPdf = createRealDigitalPdf("UNIQUE IDENTIFICATION AUTHORITY OF INDIA\nName: Aarav S. Sharma\nDOB: 15/08/1995\nGender: MALE\n5482 9102 4589");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "aadhaar.pdf", "application/pdf", aadhaarPdf), CITIZEN_ID);

        byte[] incomePdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/89412\nApplicant: Aarav S. Sharma");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "income.pdf", "application/pdf", incomePdf), CITIZEN_ID);

        byte[] castePdf = createRealDigitalPdf("CASTE CERTIFICATE\nApplicant: Aarav S. Sharma\nCategory: OBC\nCertificate No: MH/CST/2026/1122");
        applicationService.uploadDocument(appRes.getId(), "CASTE_CERT", new MockMultipartFile("file", "caste.pdf", "application/pdf", castePdf), CITIZEN_ID);

        byte[] domicilePdf = createRealDigitalPdf("DOMICILE CERTIFICATE OF MAHARASHTRA\nApplicant: Aarav S. Sharma\nResident: Pune, Maharashtra");
        applicationService.uploadDocument(appRes.getId(), "DOMICILE_CERT", new MockMultipartFile("file", "domicile.pdf", "application/pdf", domicilePdf), CITIZEN_ID);

        byte[] landPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA REVENUE 7/12 EXTRACT\nOwner: Aarav S. Sharma\nArea: 2.5 Hectares");
        applicationService.uploadDocument(appRes.getId(), "LAND_RECORD", new MockMultipartFile("file", "land.pdf", "application/pdf", landPdf), CITIZEN_ID);

        byte[] bankPdf = createRealDigitalPdf("STATE BANK OF INDIA PASSBOOK\nA/C No: 33445566778\nIFSC: SBIN0001234\nName: Aarav S. Sharma");
        applicationService.uploadDocument(appRes.getId(), "BANK_PASSBOOK", new MockMultipartFile("file", "passbook.pdf", "application/pdf", bankPdf), CITIZEN_ID);

        // Step 4: Run OCR on Income Proof
        DocumentOcrResponse ocrRes = documentOcrService.processOcr(appRes.getId(), "INCOME_PROOF", CITIZEN_ID, false);
        assertNotNull(ocrRes);
        assertEquals("Income Certificate", ocrRes.getDetectedDocumentType());
        assertEquals(160000.0, ocrRes.getExtractedFields().get("annualIncome").getValue());

        // Step 5: Submit application (Readiness is now READY_FOR_SUBMISSION)
        ApplicationResponse submittedRes = applicationService.submitApplication(appRes.getId(), CITIZEN_ID);
        assertEquals("SUBMITTED", submittedRes.getStatus());

        // Step 6: Admin starts review
        applicationReviewService.startReview(appRes.getId(), "officer_patil_01", "ROLE_ADMIN");

        // Step 7: Admin verifies all mandatory documents
        applicationReviewService.verifyDocument(appRes.getId(), "AADHAAR", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "INCOME_PROOF", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "CASTE_CERT", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "DOMICILE_CERT", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "LAND_RECORD", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "BANK_PASSBOOK", "officer_patil_01");

        // Step 8: Verify CitizenProfile received DOCUMENT_VERIFIED provenance
        CitizenProfile updatedProfile = citizenProfileRepository.findByUserId(CITIZEN_ID).orElseThrow();
        VerifiedAttribute<?> verifiedIncome = updatedProfile.getVerifiedAttributes().get("annualIncome");
        assertNotNull(verifiedIncome);
        assertEquals("DOCUMENT_VERIFIED", verifiedIncome.getSource());
        assertTrue(verifiedIncome.getVerified());
        assertEquals(160000.0, verifiedIncome.getValue());

        // Step 9: Admin approves application (triggers deterministic eligibility re-evaluation)
        ApplicationResponse approvedRes = applicationReviewService.approveApplication(appRes.getId(), "All documentation verified and eligible.", "officer_patil_01", "ROLE_ADMIN");
        assertNotNull(approvedRes);
        assertEquals("APPROVED", approvedRes.getStatus());
    }

    @Test
    @DisplayName("3. Eligibility Re-Evaluation Gate: Approval is BLOCKED if verified document attributes render applicant NOT_ELIGIBLE")
    void testEligibilityReEvaluationGate_BlocksIneligibleApproval() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, CITIZEN_ID);

        // Upload documents
        byte[] aadhaarPdf = createRealDigitalPdf("Aadhaar: 5482 9102 4589\nAarav S. Sharma");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "a.pdf", "application/pdf", aadhaarPdf), CITIZEN_ID);

        byte[] incomePdf = createRealDigitalPdf("Annual Income: Rs. 160000");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "i.pdf", "application/pdf", incomePdf), CITIZEN_ID);

        byte[] castePdf = createRealDigitalPdf("Category: OBC");
        applicationService.uploadDocument(appRes.getId(), "CASTE_CERT", new MockMultipartFile("file", "c.pdf", "application/pdf", castePdf), CITIZEN_ID);

        byte[] domicilePdf = createRealDigitalPdf("Resident of Maharashtra");
        applicationService.uploadDocument(appRes.getId(), "DOMICILE_CERT", new MockMultipartFile("file", "d.pdf", "application/pdf", domicilePdf), CITIZEN_ID);

        byte[] landPdf = createRealDigitalPdf("Land Area: 2.5 Hectares");
        applicationService.uploadDocument(appRes.getId(), "LAND_RECORD", new MockMultipartFile("file", "l.pdf", "application/pdf", landPdf), CITIZEN_ID);

        byte[] bankPdf = createRealDigitalPdf("Bank Passbook SBIN0001234");
        applicationService.uploadDocument(appRes.getId(), "BANK_PASSBOOK", new MockMultipartFile("file", "b.pdf", "application/pdf", bankPdf), CITIZEN_ID);

        applicationService.submitApplication(appRes.getId(), CITIZEN_ID);
        applicationReviewService.startReview(appRes.getId(), "officer_patil_01", "ROLE_ADMIN");

        // Admin verifies documents
        applicationReviewService.verifyDocument(appRes.getId(), "AADHAAR", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "INCOME_PROOF", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "CASTE_CERT", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "DOMICILE_CERT", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "LAND_RECORD", "officer_patil_01");
        applicationReviewService.verifyDocument(appRes.getId(), "BANK_PASSBOOK", "officer_patil_01");

        // Now simulate that verified income is updated to ₹800,000 (exceeds ₹300,000 ceiling!)
        CitizenProfile profile = citizenProfileRepository.findByUserId(CITIZEN_ID).orElseThrow();
        profile.setAnnualIncome(800000.0);
        profile.getVerifiedAttributes().put("annualIncome", VerifiedAttribute.<Double>builder()
                .value(800000.0)
                .source("DOCUMENT_VERIFIED")
                .verified(true)
                .build());
        citizenProfileRepository.save(profile);

        // Attempting to approve must fail with IllegalStateException due to NOT_ELIGIBLE re-evaluation!
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                applicationReviewService.approveApplication(appRes.getId(), "Approved", "officer_patil_01", "ROLE_ADMIN"));

        assertTrue(ex.getMessage().contains("NOT_ELIGIBLE"));
    }

    @Test
    @DisplayName("4. Rejection & Re-upload: Rejection marks CORRECTION_REQUIRED and replacement creates Version 2")
    void testRejectionAndReuploadWorkflow() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, CITIZEN_ID);

        byte[] aadhaarPdf = createRealDigitalPdf("Aadhaar: 5482 9102 4589\nAarav S. Sharma");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "a.pdf", "application/pdf", aadhaarPdf), CITIZEN_ID);

        byte[] incomePdf = createRealDigitalPdf("Annual Income: Rs. 160000");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "i.pdf", "application/pdf", incomePdf), CITIZEN_ID);

        byte[] castePdf = createRealDigitalPdf("Category: OBC");
        applicationService.uploadDocument(appRes.getId(), "CASTE_CERT", new MockMultipartFile("file", "c.pdf", "application/pdf", castePdf), CITIZEN_ID);

        byte[] domicilePdf = createRealDigitalPdf("Resident of Maharashtra");
        applicationService.uploadDocument(appRes.getId(), "DOMICILE_CERT", new MockMultipartFile("file", "d.pdf", "application/pdf", domicilePdf), CITIZEN_ID);

        byte[] landPdf = createRealDigitalPdf("Land Area: 2.5 Hectares");
        applicationService.uploadDocument(appRes.getId(), "LAND_RECORD", new MockMultipartFile("file", "l.pdf", "application/pdf", landPdf), CITIZEN_ID);

        byte[] bankPdf = createRealDigitalPdf("Bank Passbook SBIN0001234");
        applicationService.uploadDocument(appRes.getId(), "BANK_PASSBOOK", new MockMultipartFile("file", "b.pdf", "application/pdf", bankPdf), CITIZEN_ID);

        // Citizen submits application
        applicationService.submitApplication(appRes.getId(), CITIZEN_ID);

        // Admin starts review
        applicationReviewService.startReview(appRes.getId(), "officer_patil_01", "ROLE_ADMIN");

        // Admin rejects income certificate with reason
        applicationReviewService.rejectDocument(appRes.getId(), "INCOME_PROOF", "Expired certificate format", "officer_patil_01");

        Application app = applicationRepository.findById(appRes.getId()).orElseThrow();
        assertEquals(ApplicationStatus.CORRECTION_REQUIRED, app.getStatus());

        // Citizen uploads replacement
        byte[] freshIncomePdf = createRealDigitalPdf("Annual Income: Rs. 160000\nIssued 2026");
        ApplicationDocumentResponse v2Doc = applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "fresh_i.pdf", "application/pdf", freshIncomePdf), CITIZEN_ID);

        assertEquals(2, v2Doc.getVersion());
        assertEquals("PENDING", v2Doc.getVerificationStatus());
    }

    @Test
    @DisplayName("5. IDOR & Security: Unauthorized citizen is prevented from accessing another citizen's checklist or documents")
    void testIdorSecurity() {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, CITIZEN_ID);

        // Intruder attempts to fetch checklist
        assertThrows(SecurityException.class, () ->
                applicationService.getDocumentChecklist(appRes.getId(), "unauthorized_intruder_99", false));

        // Intruder attempts to submit
        assertThrows(SecurityException.class, () ->
                applicationService.submitApplication(appRes.getId(), "unauthorized_intruder_99"));
    }
}
