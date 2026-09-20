package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
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
public class Phase11aRealDocumentValidationTest {

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
    private com.schemebridge.scheme.repository.CitizenVaultDocumentRepository citizenVaultDocumentRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationReviewService applicationReviewService;

    @Autowired
    private DocumentOcrService documentOcrService;

    @Autowired
    private DocumentOcrProvider ocrProvider;

    private Scheme testScheme;
    private static final String TEST_USER_ID = "test_citizen_65";
    private static final String SCHEME_CODE = "SCH-PHASE11A-VALIDATION";

    @BeforeEach
    void setUp() {
        // Clean up test records
        applicationDocumentRepository.deleteAll();
        applicationRepository.deleteAll();
        ocrResultRepository.deleteAll();
        citizenVaultDocumentRepository.deleteAll();
        schemeRepository.findBySchemeCode(SCHEME_CODE).ifPresent(schemeRepository::delete);
        citizenProfileRepository.findByUserId(TEST_USER_ID).ifPresent(citizenProfileRepository::delete);

        // Required Documents Checklist for Scheme
        RequiredDocument reqIncome = RequiredDocument.builder()
                .documentCode("INCOME_PROOF")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .description(MultilingualText.builder().english("Official Tahasildar Income Certificate").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG", "PNG"))
                .issuingAuthority("Revenue Department")
                .build();

        RequiredDocument reqAadhaar = RequiredDocument.builder()
                .documentCode("AADHAAR")
                .name(MultilingualText.builder().english("Aadhaar Card").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG", "PNG"))
                .issuingAuthority("UIDAI")
                .build();

        testScheme = Scheme.builder()
                .schemeCode(SCHEME_CODE)
                .slug("phase11a-validation-scheme")
                .title(MultilingualText.builder().english("Phase 11A Validation Scheme").build())
                .description(MultilingualText.builder().english("Real document validation test scheme").build())
                .status(SchemeStatus.ACTIVE)
                .requiredDocuments(List.of(reqIncome, reqAadhaar))
                .version(1)
                .build();

        schemeRepository.save(testScheme);

        // Real Citizen Profile
        CitizenProfile profile = CitizenProfile.builder()
                .userId(TEST_USER_ID)
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
        citizenProfileRepository.findByUserId(TEST_USER_ID).ifPresent(citizenProfileRepository::delete);
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
    @DisplayName("1. Document Checklist & Application Creation: Correctly initializes required document records")
    void testDocumentChecklist_AndApplicationCreation() {
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode(SCHEME_CODE)
                .build();

        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        assertNotNull(appRes);
        assertEquals(SCHEME_CODE, appRes.getSchemeCode());
        assertEquals("DOCUMENTS_PENDING", appRes.getStatus());

        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(appRes.getId());
        assertEquals(2, docs.size());

        ApplicationDocument incomeDoc = docs.stream().filter(d -> d.getDocumentCode().equals("INCOME_PROOF")).findFirst().orElse(null);
        assertNotNull(incomeDoc);
        assertTrue(incomeDoc.isMandatory());
        assertFalse(incomeDoc.isUploaded());
        assertEquals(DocumentVerificationStatus.PENDING, incomeDoc.getVerificationStatus());
    }

    @Test
    @DisplayName("2. Citizen Document Upload: Stores valid PDF in GridFS and tracks metadata")
    void testCitizenUpload_ValidPdf() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        byte[] pdfBytes = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/9001");
        MockMultipartFile file = new MockMultipartFile("file", "income_cert.pdf", "application/pdf", pdfBytes);

        ApplicationDocumentResponse docRes = applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", file, TEST_USER_ID);

        assertNotNull(docRes);
        assertTrue(docRes.isUploaded());
        assertEquals("income_cert.pdf", docRes.getFileName());
        assertEquals(1, docRes.getVersion());
        assertEquals("PENDING", docRes.getVerificationStatus());
    }

    @Test
    @DisplayName("3. Negative Upload Validation: Blocks unsupported extensions and oversized files")
    void testNegativeUploadValidation() {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        // Unsupported extension
        MockMultipartFile exeFile = new MockMultipartFile("file", "exploit.exe", "application/octet-stream", new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", exeFile, TEST_USER_ID));

        // Oversized file (>5MB)
        byte[] largeBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);
        assertThrows(IllegalArgumentException.class, () ->
                applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", largeFile, TEST_USER_ID));
    }

    @Test
    @DisplayName("4. Real Digital PDF OCR: Extracts structured fields, confidence score, and raw text")
    void testRealPdfOcr_Extraction() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        byte[] realPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nREVENUE DIVISION PUNE\nCertificate No: MH/INC/2026/89412\n" +
                "Applicant: Aarav S. Sharma\nAnnual Income: Rs. 160000 per annum\nIssued on 15/04/2026");
        MockMultipartFile file = new MockMultipartFile("file", "income_cert.pdf", "application/pdf", realPdf);
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", file, TEST_USER_ID);

        DocumentOcrResponse ocrRes = documentOcrService.processOcr(appRes.getId(), "INCOME_PROOF", TEST_USER_ID, false);

        assertNotNull(ocrRes);
        assertEquals(OcrExtractionStatus.SUCCESS, ocrRes.getExtractionStatus());
        assertEquals("Income Certificate", ocrRes.getDetectedDocumentType());
        assertTrue(ocrRes.getOverallConfidence() >= 0.85);

        Map<String, ExtractedFieldDetail> fields = ocrRes.getExtractedFields();
        assertNotNull(fields);
        assertTrue(fields.containsKey("annualIncome"));
        assertEquals(160000.0, fields.get("annualIncome").getValue());
        assertEquals("OCR_EXTRACTED", fields.get("annualIncome").getSource());
        assertFalse(fields.get("annualIncome").isVerified());

        assertTrue(fields.containsKey("certificateNumber"));
        assertEquals("MH/INC/2026/89412", fields.get("certificateNumber").getValue());
    }

    @Test
    @DisplayName("5. Admin Review & Provenance Promotion: Promotes OCR attributes to DOCUMENT_VERIFIED in CitizenProfile")
    void testAdminReview_AndProvenancePromotion() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        // Upload both mandatory documents
        byte[] incPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/89412");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "inc.pdf", "application/pdf", incPdf), TEST_USER_ID);

        byte[] adhPdf = createRealDigitalPdf("UNIQUE IDENTIFICATION AUTHORITY OF INDIA\nName: Aarav S. Sharma\nDOB: 15/08/1995\nGender: MALE\n5482 9102 4589");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "adh.pdf", "application/pdf", adhPdf), TEST_USER_ID);

        // Process OCR on income proof
        documentOcrService.processOcr(appRes.getId(), "INCOME_PROOF", TEST_USER_ID, false);

        // Submit Application
        applicationService.submitApplication(appRes.getId(), TEST_USER_ID);

        // Admin starts review
        applicationReviewService.startReview(appRes.getId(), "admin_officer_1", "ROLE_ADMIN");

        // Admin verifies income document
        applicationReviewService.verifyDocument(appRes.getId(), "INCOME_PROOF", "admin_officer_1");

        // Verify CitizenProfile updated with DOCUMENT_VERIFIED provenance
        CitizenProfile updatedProfile = citizenProfileRepository.findByUserId(TEST_USER_ID).orElseThrow();
        VerifiedAttribute<?> attr = updatedProfile.getVerifiedAttributes().get("annualIncome");
        assertNotNull(attr);
        assertEquals(160000.0, attr.getValue());
        assertEquals("DOCUMENT_VERIFIED", attr.getSource());
        assertTrue(attr.getVerified());
        assertNotNull(attr.getVerifiedAt());
    }

    @Test
    @DisplayName("6. Document Versioning & Re-upload: Version increments and state transitions to CORRECTION_REQUIRED")
    void testDocumentVersioning_AndReupload() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        byte[] incPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/89412");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "inc.pdf", "application/pdf", incPdf), TEST_USER_ID);

        byte[] adhPdf = createRealDigitalPdf("UNIQUE IDENTIFICATION AUTHORITY OF INDIA\nName: Aarav S. Sharma\nDOB: 15/08/1995\nGender: MALE\n5482 9102 4589");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "adh.pdf", "application/pdf", adhPdf), TEST_USER_ID);

        applicationService.submitApplication(appRes.getId(), TEST_USER_ID);
        applicationReviewService.startReview(appRes.getId(), "admin_officer_1", "ROLE_ADMIN");

        // Admin rejects income document
        applicationReviewService.rejectDocument(appRes.getId(), "INCOME_PROOF", "Blurry scan", "admin_officer_1");

        Application app = applicationRepository.findById(appRes.getId()).orElseThrow();
        assertEquals(ApplicationStatus.CORRECTION_REQUIRED, app.getStatus());

        // Citizen re-uploads replacement document (Version 2)
        byte[] newPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/99999");
        ApplicationDocumentResponse v2Res = applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "inc_v2.pdf", "application/pdf", newPdf), TEST_USER_ID);

        assertEquals(2, v2Res.getVersion());
        assertEquals("PENDING", v2Res.getVerificationStatus());
        assertEquals("inc_v2.pdf", v2Res.getFileName());
    }

    @Test
    @DisplayName("7. Mandatory Document Approval Gate: Approval blocked until all mandatory documents are verified")
    void testMandatoryDocumentApprovalGate() throws IOException {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        byte[] incPdf = createRealDigitalPdf("GOVERNMENT OF MAHARASHTRA\nAnnual Income: Rs. 160000\nCertificate No: MH/INC/2026/89412");
        applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", new MockMultipartFile("file", "inc.pdf", "application/pdf", incPdf), TEST_USER_ID);

        byte[] adhPdf = createRealDigitalPdf("UNIQUE IDENTIFICATION AUTHORITY OF INDIA\nName: Aarav S. Sharma\nDOB: 15/08/1995\nGender: MALE\n5482 9102 4589");
        applicationService.uploadDocument(appRes.getId(), "AADHAAR", new MockMultipartFile("file", "adh.pdf", "application/pdf", adhPdf), TEST_USER_ID);

        applicationService.submitApplication(appRes.getId(), TEST_USER_ID);
        applicationReviewService.startReview(appRes.getId(), "admin_officer_1", "ROLE_ADMIN");

        // Verify only Income Proof (Aadhaar still pending)
        applicationReviewService.verifyDocument(appRes.getId(), "INCOME_PROOF", "admin_officer_1");

        // Must fail approval because Aadhaar is mandatory and not verified yet
        assertThrows(IllegalStateException.class, () ->
                applicationReviewService.approveApplication(appRes.getId(), "Approved", "admin_officer_1", "ROLE_ADMIN"));

        // Verify Aadhaar as well
        applicationReviewService.verifyDocument(appRes.getId(), "AADHAAR", "admin_officer_1");

        // Now approval must succeed
        ApplicationResponse approved = applicationReviewService.approveApplication(appRes.getId(), "All verified", "admin_officer_1", "ROLE_ADMIN");
        assertEquals("APPROVED", approved.getStatus());
    }

    @Test
    @DisplayName("8. IDOR Security: Unauthorized citizen cannot access or upload to another's application")
    void testIdorSecurity() {
        CreateApplicationRequest req = CreateApplicationRequest.builder().schemeCode(SCHEME_CODE).build();
        ApplicationResponse appRes = applicationService.createApplication(req, TEST_USER_ID);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        // Intruder tries to upload
        assertThrows(SecurityException.class, () ->
                applicationService.uploadDocument(appRes.getId(), "INCOME_PROOF", file, "intruder_user_99"));

        // Intruder tries to run OCR
        assertThrows(SecurityException.class, () ->
                documentOcrService.processOcr(appRes.getId(), "INCOME_PROOF", "intruder_user_99", false));
    }
}
