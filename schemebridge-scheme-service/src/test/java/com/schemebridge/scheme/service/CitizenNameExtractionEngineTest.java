package com.schemebridge.scheme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Universal Citizen/Holder Name Extraction Engine Test Suite.
 *
 * Verifies the 15+ scenario matrix (A through O) mandated for universal,
 * layout-grounded, zero-profile-leakage citizen name extraction.
 */
@ExtendWith(MockitoExtension.class)
class CitizenNameExtractionEngineTest {

    private CitizenNameExtractionEngine engine;
    private DocumentExtractionService extractionService;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @BeforeEach
    void setUp() {
        engine = new CitizenNameExtractionEngine();
        extractionService = new DocumentExtractionService(
                "",
                "gemini-1.5-flash",
                new ObjectMapper(),
                citizenProfileRepository,
                engine
        );
    }

    // ── Matrix A: Aadhaar ─────────────────────────────────────────────────────
    @Test
    @DisplayName("Matrix A1: Aadhaar extraction resolves Nivetha from layout")
    void testAadhaar_Nivetha() {
        String content = "GOVERNMENT OF INDIA\n" +
                "Nivetha\n" +
                "DOB: 27/11/2006\n" +
                "FEMALE\n" +
                "9876 5432 1098\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "AADHAAR");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Nivetha", result.holderName());
        assertTrue(result.confidence() >= 0.85);
    }

    @Test
    @DisplayName("Matrix A2: Aadhaar extraction resolves Lathika from standard UIDAI To layout")
    void testAadhaar_Lathika_ToLayout() {
        String content = "Government of India\n" +
                "Unique Identification Authority of India\n" +
                "To\n" +
                "Lathika\n" +
                "D/O: Kumar, 428, KOZHAI STREET, Kozhai\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "AADHAAR");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
    }

    // ── Matrix B: Income Certificate ──────────────────────────────────────────
    @Test
    @DisplayName("Matrix B: Income Certificate resolves Lathika")
    void testIncomeCertificate_Lathika() {
        String content = "Tamil Nadu Government\n" +
                "Revenue Administration, Cuddalore District\n" +
                "Income Certificate\n" +
                "Certificate No: TN-INC-2024-8899\n" +
                "This is to certify that Selvi Lathika daughter of Thiru Kumar residing at Kozhai\n" +
                "annual family income is Rs. 75,000/annum\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
    }

    // ── Matrix C: Community Certificate ───────────────────────────────────────
    @Test
    @DisplayName("Matrix C: Community Certificate repairs fragmented candidate (Selvi L athika -> Lathika)")
    void testCommunityCertificate_SelviLAthika() {
        String content = "Government of Tamil Nadu\n" +
                "Revenue Department\n" +
                "Community Certificate\n" +
                "This is to certify that Selvi L athika daughter of Thiru Kumar residing at Kozhai\n" +
                "belongs to Backward Class Community\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "COMMUNITY_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
    }

    // ── Matrix D: Caste Certificate (Holder vs. Father) ──────────────────────
    @Test
    @DisplayName("Matrix D: Caste Certificate selects Applicant Lathika, rejecting Father Kumar")
    void testCasteCertificate_ApplicantLathika_FatherKumar() {
        String content = "Government of Tamil Nadu\n" +
                "Caste Certificate\n" +
                "Applicant Name: Lathika\n" +
                "Father Name: Kumar\n" +
                "District: Cuddalore\n" +
                "Certificate No: TN-CST-2024-1122\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "CASTE_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName(), "Holder must be Lathika, not father Kumar");
        assertNotEquals("Kumar", result.holderName());
    }

    // ── Matrix E: Domicile Certificate (Holder vs. Authority) ─────────────────
    @Test
    @DisplayName("Matrix E: Domicile Certificate selects Holder Lathika, rejecting Authority Kumar")
    void testDomicileCertificate_HolderLathika_AuthorityKumar() {
        String content = "Government of Tamil Nadu\n" +
                "Revenue Department\n" +
                "Domicile Certificate\n" +
                "Certificate Holder: Lathika\n" +
                "Authorized Signatory: Tahsildar Arun Kumar\n" +
                "Revenue Authority Office, Cuddalore\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "DOMICILE_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
        assertNotEquals("Kumar", result.holderName());
    }

    // ── Matrix F: Different Applicant Identity Mismatch ───────────────────────
    @Test
    @DisplayName("Matrix F: Uploaded Priya document vs. Applicant Lathika produces MISMATCH")
    void testDifferentApplicant_Mismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Government of Tamil Nadu\n" +
                "Income Certificate\n" +
                "This is to certify that Selvi Priya daughter of Thiru Murugan\n" +
                "annual income is Rs. 50000/annum\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "priya_income.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Priya", response.getFields().get("holderName").getValue());
        assertNotNull(response.getIdentityVerification());
        assertEquals("MISMATCH", response.getIdentityVerification().getStatus());
        assertFalse(response.getIdentityVerification().isNameMatch());
        assertFalse(response.getIdentityVerification().isOverallMatch());
    }

    // ── Matrix G: Multiple People in Document ──────────────────────────────────
    @Test
    @DisplayName("Matrix G: Multiple people (Self Lathika, Father Kumar, Mother Priya) correctly selects Lathika")
    void testMultiplePeople_FamilyTable() {
        String content = "Income Certificate\n" +
                "Certificate No: TN-4202403185459\n" +
                "Name of family Member\n" +
                "Kumar Father\n" +
                "Priya Mother\n" +
                "Lathika Self\n" +
                "Total Annual Income is RS. 72000/annum\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
    }

    // ── Matrix H: OCR Word Fragmentation ──────────────────────────────────────
    @Test
    @DisplayName("Matrix H1: N ivetha -> Nivetha")
    void testOcrFragmentation_Nivetha() {
        String normalized = engine.normalizePersonNameCandidate("N ivetha");
        assertEquals("Nivetha", normalized);
    }

    @Test
    @DisplayName("Matrix H2: Niv etha -> Nivetha")
    void testOcrFragmentation_Nivetha_Split2() {
        String normalized = engine.normalizePersonNameCandidate("Niv etha");
        assertEquals("Nivetha", normalized);
    }

    @Test
    @DisplayName("Matrix H3: L athika -> Lathika")
    void testOcrFragmentation_Lathika() {
        String normalized = engine.normalizePersonNameCandidate("L athika");
        assertEquals("Lathika", normalized);
    }

    @Test
    @DisplayName("Matrix H4: La thika -> Lathika")
    void testOcrFragmentation_Lathika_Split2() {
        String normalized = engine.normalizePersonNameCandidate("La thika");
        assertEquals("Lathika", normalized);
    }

    // ── Matrix I: Location False Positive Protection ──────────────────────────
    @Test
    @DisplayName("Matrix I: 'Lathika Street' must NOT become holder name Lathika")
    void testLocationFalsePositive_Street() {
        String content = "Government of Tamil Nadu\n" +
                "Revenue Department\n" +
                "Address: 12, Lathika Street, Kozhai, Cuddalore\n" +
                "District Office\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "GOVERNMENT_DOCUMENT");

        assertNotNull(result);
        assertNotEquals("Lathika", result.holderName());
        assertNotEquals("Lathika Street", result.holderName());
        assertTrue("UNCERTAIN".equals(result.status()) || "NOT_FOUND".equals(result.status()) || result.holderName() == null);
    }

    // ── Matrix J: Authority False Positive Protection ─────────────────────────
    @Test
    @DisplayName("Matrix J: 'Lathika Revenue Authority' must NOT become holder name")
    void testAuthorityFalsePositive_RevenueAuthority() {
        String content = "Issued by Lathika Revenue Authority, Cuddalore District Office\n" +
                "Official Document Certification\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "GOVERNMENT_DOCUMENT");

        assertNotNull(result);
        assertNull(result.holderName(), "Authority string must never become document holder");
        assertEquals("UNCERTAIN", result.status());
    }

    // ── Matrix K: Garbage OCR Protection ──────────────────────────────────────
    @Test
    @DisplayName("Matrix K: Random garbage OCR fragments return null / UNCERTAIN")
    void testGarbageOcr_ReturnsUncertain() {
        String content = "asdf hjkl qwrty\n" +
                "zxvcb nm,./;' 9876\n" +
                "12345 67890\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "GENERAL");

        assertNotNull(result);
        assertNull(result.holderName());
        assertEquals("UNCERTAIN", result.status());
    }

    // ── Matrix L: Multi-Word Names ────────────────────────────────────────────
    @Test
    @DisplayName("Matrix L: Legitimate multi-word name 'Lathika Kumar' is preserved unchanged")
    void testMultiWordName_Preserved() {
        String normalized = engine.normalizePersonNameCandidate("Lathika Kumar");
        assertEquals("Lathika Kumar", normalized);

        String content = "Applicant Name: Lathika Kumar\n" +
                "Income Certificate\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("Lathika Kumar", result.holderName());
    }

    // ── Matrix M: Initial Names ───────────────────────────────────────────────
    @Test
    @DisplayName("Matrix M1: Trailing initial 'Lathika K' is preserved unchanged")
    void testInitialName_TrailingInitial_Preserved() {
        String normalized = engine.normalizePersonNameCandidate("Lathika K");
        assertEquals("Lathika K", normalized);

        String content = "Name: Lathika K\n" +
                "DOB: 22/03/2007\n" +
                "Female\n";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(content, null, "AADHAAR");

        assertNotNull(result);
        assertEquals("Lathika K", result.holderName());
    }

    @Test
    @DisplayName("Matrix M2: Leading initial with period 'K. Lathika' is preserved unchanged")
    void testInitialName_LeadingInitialWithPeriod_Preserved() {
        String normalized = engine.normalizePersonNameCandidate("K. Lathika");
        assertEquals("K. Lathika", normalized);
    }

    @Test
    @DisplayName("Matrix M3: Leading initial without period 'K Lathika' is preserved unchanged")
    void testInitialName_LeadingInitialWithoutPeriod_Preserved() {
        String normalized = engine.normalizePersonNameCandidate("K Lathika");
        assertEquals("K Lathika", normalized);
    }

    // ── Matrix N: PDF + OCR Disagreement Reconciliation ───────────────────────
    @Test
    @DisplayName("Matrix N: PDF text produces noise 'Quarl', Visual OCR produces 'Nivetha' near DOB/Gender -> Nivetha is selected")
    void testPdfOcrDisagreement_NivethaWinsOverQuarl() {
        String pdfText = "Quarl\n" +
                "Tamil Nadu Revenue\n" +
                "1234 5678 9012\n";

        String visualOcrText = "Nivetha\n" +
                "DOB: 27/11/2006\n" +
                "FEMALE\n" +
                "9876 5432 1098\n";

        CitizenNameExtractionEngine.OcrLine nameLine = new CitizenNameExtractionEngine.OcrLine(
                "Nivetha", 418.0, 594.0, 169.0, 31.0,
                List.of(new CitizenNameExtractionEngine.OcrWord("Nivetha", 418.0, 594.0, 169.0, 31.0))
        );
        CitizenNameExtractionEngine.OcrLine dobLine = new CitizenNameExtractionEngine.OcrLine(
                "DOB: 27/11/2006", 416.0, 669.0, 198.0, 29.0,
                List.of(new CitizenNameExtractionEngine.OcrWord("27/11/2006", 416.0, 669.0, 198.0, 29.0))
        );
        CitizenNameExtractionEngine.StructuredOcrData ocrData =
                new CitizenNameExtractionEngine.StructuredOcrData(visualOcrText, List.of(nameLine, dobLine));

        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(pdfText, visualOcrText, ocrData, "AADHAAR");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Nivetha", result.holderName(), "Nivetha supported by visual OCR + DOB/Gender must beat Quarl");
        assertNotEquals("Quarl", result.holderName());
    }

    // ── Matrix O: Real Document Fixtures ──────────────────────────────────────
    @Test
    @DisplayName("Matrix O1: Real screenshot fixture extracts genuine citizen and verifies applicant")
    void testRealFixture_Screenshot() throws Exception {
        File file = new File("e:/SCHEMEBRIDGE/test-documents/Screenshot_20260919-091359.png");
        if (!file.exists()) return;

        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "Screenshot_20260919-091359.png", "image/png", bytes
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(mockFile, "AADHAAR", "Identity Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika K", response.getFields().get("holderName").getValue());
        assertEquals("FOUND", response.getFields().get("holderName").getStatus());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("Matrix O2: Real PDF fixture Lathika Originals-5.pdf extracts Lathika")
    void testRealFixture_LathikaOriginals5() throws Exception {
        File file = new File("e:/SCHEMEBRIDGE/test-documents/Lathika Originals-5.pdf");
        if (!file.exists()) return;

        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "Lathika Originals-5.pdf", "application/pdf", bytes
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(mockFile, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("Matrix O3: Real PDF fixture Lathika Originals-7.pdf extracts Lathika")
    void testRealFixture_LathikaOriginals7() throws Exception {
        File file = new File("e:/SCHEMEBRIDGE/test-documents/Lathika Originals-7.pdf");
        if (!file.exists()) return;

        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "Lathika Originals-7.pdf", "application/pdf", bytes
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(mockFile, "AADHAAR", "Identity Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    // ── Nivetha Specific Regression Verification ──────────────────────────────
    @Test
    @DisplayName("Regression Criterion: Nivetha image with applicant 'Nivetha N' produces MATCH, never Quarl")
    void testRegression_Nivetha_ApplicantNivethaN() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-nivetha")
                .displayName("Nivetha N")
                .dob(LocalDate.of(2006, 11, 27))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-nivetha")).thenReturn(Optional.of(applicantProfile));

        String rawContent = "Nivetha\n" +
                "DOB: 27/11/2006\n" +
                "FEMALE\n" +
                "1234 5678 9012\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "nivetha_aadhaar.png", "image/png", rawContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(file, "AADHAAR", "Identity Proof", "user-nivetha");

        assertNotNull(response);
        assertEquals("Nivetha", response.getFields().get("holderName").getValue());
        assertNotEquals("Quarl", response.getFields().get("holderName").getValue());
        assertNotNull(response.getIdentityVerification());
        assertTrue(response.getIdentityVerification().isNameMatch());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("Regression Criterion: OCR hyphen artifact Nv-etha with Quarl on gender line extracts Nivetha, never Quarl")
    void testRegression_Nivetha_OcrHyphenArtifact_NeverQuarl() {
        String ocrRawText = "Nv-etha\n" +
                "19\" \"åT/DOB: 27/11/2006\n" +
                "Quarl FEMALE\n" +
                "7334 3804 4223\n" +
                "•gym i, 6T6TTSJ 016DL—turrmCD.";

        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(ocrRawText, null, "AADHAAR");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Nivetha", result.holderName());
        assertNotEquals("Quarl", result.holderName());
        assertNotEquals("Nvetha", result.holderName());
    }

    @Test
    @DisplayName("Prompt Scenario TEST 6 & 7: Father Nivetha does NOT match applicant Nivetha")
    void testApplicantPriya_FatherNivetha() {
        String cert = "Applicant: Priya\nFather: Nivetha\nAnnual Income: Rs. 50,000";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(cert, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("Priya", result.holderName());
        assertNotEquals("Nivetha", result.holderName());

        // Identity verification against Nivetha N profile must be MISMATCH
        CitizenProfile nivethaProfile = CitizenProfile.builder()
                .userId("user-nivetha")
                .displayName("Nivetha N")
                .build();
        var idRes = extractionService.verifyIdentityWithProfile("INCOME_CERTIFICATE", result.holderName(), null, null, nivethaProfile);
        assertEquals("MISMATCH", idRes.getStatus());
        assertFalse(idRes.isOverallMatch());
    }

    @Test
    @DisplayName("Prompt Scenario TEST 8 & 9: OCR fragmentation repairs N ivetha -> Nivetha")
    void testOcrFragmentation_NivethaVariants() {
        assertEquals("Nivetha", engine.normalizePersonNameCandidate("N ivetha"));
        assertEquals("Nivetha", engine.normalizePersonNameCandidate("Ni vetha"));
        assertEquals("Nivetha", engine.normalizePersonNameCandidate("Niv etha"));
        assertEquals("Nivetha", engine.normalizePersonNameCandidate("Nive tha"));
        assertEquals("Nivetha", engine.normalizePersonNameCandidate("Nivet ha"));

        assertEquals("Lathika", engine.normalizePersonNameCandidate("L athika"));
        assertEquals("Lathika", engine.normalizePersonNameCandidate("La thika"));
        assertEquals("Lathika", engine.normalizePersonNameCandidate("Lat hika"));
        assertEquals("Lathika", engine.normalizePersonNameCandidate("Lath ika"));
        assertEquals("Lathika", engine.normalizePersonNameCandidate("Lathik a"));
    }

    @Test
    @DisplayName("Prompt Scenario TEST 14-16: Multi-word and initials preservation")
    void testMultiWordAndInitialsPreservation() {
        assertEquals("Lathika Kumar", engine.normalizePersonNameCandidate("Lathika Kumar"));
        assertEquals("Lathika K", engine.normalizePersonNameCandidate("Lathika K"));
        assertEquals("K. Lathika", engine.normalizePersonNameCandidate("K. Lathika"));
        assertEquals("K Lathika", engine.normalizePersonNameCandidate("K Lathika"));
        assertEquals("Nivetha N", engine.normalizePersonNameCandidate("Nivetha N"));
    }

    @Test
    @DisplayName("Prompt Scenario TEST 1 & 2: Identity matching rules for Nivetha and Nivetha N")
    void testIdentityMatchingRules() {
        assertTrue(extractionService.isNameConsistent("Nivetha N", "Nivetha"));
        assertTrue(extractionService.isNameConsistent("N. Nivetha", "Nivetha"));
        assertTrue(extractionService.isNameConsistent("Nivetha.N", "Nivetha"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Lathika"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Lathika K"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "K. Lathika"));

        // Different applicants must strictly be MISMATCH
        assertFalse(extractionService.isNameConsistent("Lathika K", "Nivetha"));
        assertFalse(extractionService.isNameConsistent("Nivetha N", "Kumar"));
        assertFalse(extractionService.isNameConsistent("Lathika.K", "Priya"));
        assertFalse(extractionService.isNameConsistent("Nivetha N", "Quarl"));
    }

    @Test
    @DisplayName("Real Fixture: Screenshot_Nivetha.png visual OCR extracts Nivetha, never Quarl")
    void testRealFixture_NivethaScreenshot() {
        File file = new File("e:/SCHEMEBRIDGE/test-documents/Screenshot_Nivetha.png");
        if (!file.exists()) return;

        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-nivetha")
                .displayName("Nivetha N")
                .dob(LocalDate.of(2006, 11, 27))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-nivetha")).thenReturn(Optional.of(applicantProfile));

        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            return;
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "Screenshot_Nivetha.png", "image/png", bytes
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(mockFile, "AADHAAR", "Identity Proof", "user-nivetha");

        assertNotNull(response);
        assertEquals("Nivetha", response.getFields().get("holderName").getValue());
        assertNotEquals("Quarl", response.getFields().get("holderName").getValue());
        assertNotNull(response.getIdentityVerification());
        assertTrue(response.getIdentityVerification().isNameMatch());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("Multilingual Matrix: Hindi document with नाम, जन्म तारीख, पुरुष extracts name correctly")
    void testMultilingual_HindiDocument() {
        String doc = "भारत सरकार\n" +
                "आवेदक का नाम: Aditya Kumar\n" +
                "पिता का नाम: Ramesh Kumar\n" +
                "जन्म तारीख: 14/08/2005\n" +
                "पुरुष";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "AADHAAR");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Aditya Kumar", result.holderName());
    }

    @Test
    @DisplayName("Multilingual Matrix: Tamil certification statement extracts holder name, excludes relationship")
    void testMultilingual_TamilCertifyStatement() {
        String doc = "வருவாய்த்துறை\n" +
                "சான்றிதழ் அளிக்கப்படுகிறது செல்வி Lathika தந்தை Thiru Kumar\n" +
                "வருமானம்: ரூ. 45,000";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Lathika", result.holderName());
        assertNotEquals("Kumar", result.holderName());
    }

    @Test
    @DisplayName("Multilingual Matrix: Telugu document with పేరు and పుట్టిన తేదీ")
    void testMultilingual_TeluguDocument() {
        String doc = "ప్రభుత్వ ధృవీకరణ పత్రం\n" +
                "పేరు: Ravi S\n" +
                "తండ్రి పేరు: Mohan S\n" +
                "పుట్టిన తేదీ: 10/05/2004\n" +
                "పురుషుడు";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "GOVERNMENT_DOCUMENT");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Ravi S", result.holderName());
        assertNotEquals("Mohan S", result.holderName());
    }

    @Test
    @DisplayName("Multilingual Matrix: Kannada document with ಹೆಸರು and ಹುಟ್ಟಿದ ದಿನಾಂಕ")
    void testMultilingual_KannadaDocument() {
        String doc = "ಸರ್ಕಾರಿ ಪ್ರಮಾಣಪತ್ರ\n" +
                "ಅರ್ಜಿದಾರರ ಹೆಸರು: Ananya R\n" +
                "ಹುಟ್ಟಿದ ದಿನಾಂಕ: 15/01/2005\n" +
                "ಮಹಿಳೆ";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "GOVERNMENT_DOCUMENT");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Ananya R", result.holderName());
    }

    @Test
    @DisplayName("Multi-Person Protection: Applicant Priya, Mother Lathika, Father Kumar -> extracts Priya")
    void testMultiPerson_ApplicantMotherFather() {
        String doc = "Applicant: Priya\nMother: Lathika\nFather: Kumar\nAnnual Income: Rs. 60,000";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "INCOME_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Priya", result.holderName());
        assertNotEquals("Lathika", result.holderName());
        assertNotEquals("Kumar", result.holderName());
    }

    @Test
    @DisplayName("Wife of Relationship: certifies Kum. Priya wife of Thiru Ramesh -> extracts Priya")
    void testRelationship_WifeOf() {
        String doc = "This is to certify that Kum. Priya wife of Thiru Ramesh residing at Door No 12";
        CitizenNameExtractionEngine.ExtractionResult result =
                engine.extractCitizenHolderName(doc, null, "COMMUNITY_CERTIFICATE");

        assertNotNull(result);
        assertEquals("FOUND", result.status());
        assertEquals("Priya", result.holderName());
        assertNotEquals("Ramesh", result.holderName());
    }

    @Test
    @DisplayName("Noise Rejection: Authority, Location, and Metadata must return UNCERTAIN, never random name")
    void testNoiseRejection_AuthorityAndLocation() {
        String doc1 = "Revenue Divisional Officer\nTamil Nadu Government\nSecretariat Chennai";
        CitizenNameExtractionEngine.ExtractionResult r1 =
                engine.extractCitizenHolderName(doc1, null, "GENERAL");
        assertEquals("UNCERTAIN", r1.status());
        assertNull(r1.holderName());

        String doc2 = "Cuddalore District\nTamil Nadu\nSrimushnam Taluk Kozhai Village";
        CitizenNameExtractionEngine.ExtractionResult r2 =
                engine.extractCitizenHolderName(doc2, null, "GENERAL");
        assertEquals("UNCERTAIN", r2.status());
        assertNull(r2.holderName());

        String doc3 = "number details remarks validity total date annual rupees";
        CitizenNameExtractionEngine.ExtractionResult r3 =
                engine.extractCitizenHolderName(doc3, null, "GENERAL");
        assertEquals("UNCERTAIN", r3.status());
        assertNull(r3.holderName());
    }

    @Test
    @DisplayName("Uncertain Extraction: verifyIdentityWithProfile returns UNCERTAIN, NOT MISMATCH")
    void testUncertainExtraction_ReturnsUncertainInVerification() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika K")
                .build();

        var res = extractionService.verifyIdentityWithProfile("INCOME_CERTIFICATE", null, null, null, profile);
        assertNotNull(res);
        assertEquals("UNCERTAIN", res.getStatus());
        assertFalse(res.isNameMatch());
        assertFalse(res.isOverallMatch());
        assertNull(res.getDobMatch());
        assertTrue(res.getFailureReason().contains("Unable to reliably identify the document holder"));
    }

    @Test
    @DisplayName("Name-Only Document Verification: DOB is null and overallMatch is true on name match")
    void testNameOnlyDocumentVerification() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        var res = extractionService.verifyIdentityWithProfile("INCOME_CERTIFICATE", "Lathika", null, null, profile);
        assertNotNull(res);
        assertEquals("MATCH", res.getStatus());
        assertTrue(res.isNameMatch());
        assertTrue(res.isOverallMatch());
        assertNull(res.getDobMatch(), "Name-only documents must have dobMatch as null");
    }

    @Test
    @DisplayName("Cross-Validation: Real Fixture Nivetha Screenshot against Lathika and Priya is MISMATCH")
    void testRealFixture_NivethaAgainstDifferentCitizens() {
        File file = new File("e:/SCHEMEBRIDGE/test-documents/Screenshot_Nivetha.png");
        if (!file.exists()) return;

        CitizenProfile lathikaProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(lathikaProfile));

        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            return;
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "Screenshot_Nivetha.png", "image/png", bytes
        );

        StructuredDocumentExtractionResponse response =
                extractionService.extractAndVerify(mockFile, "AADHAAR", "Identity Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Nivetha", response.getFields().get("holderName").getValue());
        assertNotNull(response.getIdentityVerification());
        assertFalse(response.getIdentityVerification().isNameMatch());
        assertFalse(response.getIdentityVerification().isOverallMatch());
        assertEquals("MISMATCH", response.getIdentityVerification().getStatus());
    }
}

