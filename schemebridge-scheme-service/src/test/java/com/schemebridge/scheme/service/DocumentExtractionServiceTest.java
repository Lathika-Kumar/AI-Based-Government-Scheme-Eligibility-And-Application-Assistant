package com.schemebridge.scheme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.document.CitizenProfile;
import com.schemebridge.scheme.document.CitizenVaultDocument;
import com.schemebridge.scheme.document.DocumentVerificationStatus;
import com.schemebridge.scheme.enums.DetailedDocumentStatus;
import com.schemebridge.scheme.dto.response.DocumentFieldExtraction;
import com.schemebridge.scheme.dto.response.IdentityVerificationResult;
import com.schemebridge.scheme.dto.response.StructuredDocumentExtractionResponse;
import com.schemebridge.scheme.repository.CitizenProfileRepository;
import com.schemebridge.scheme.repository.CitizenVaultDocumentRepository;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionServiceTest {

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private CitizenVaultDocumentRepository vaultDocumentRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    private ObjectMapper objectMapper;
    private DocumentExtractionService extractionService;
    private CitizenVaultDocumentService vaultDocumentService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        extractionService = new DocumentExtractionService(
                "", // empty API key to trigger deterministic grounded extraction
                "gemini-1.5-flash",
                objectMapper,
                citizenProfileRepository
        );

        vaultDocumentService = new CitizenVaultDocumentService(
                vaultDocumentRepository,
                documentStorageService,
                extractionService
        );
    }

    @Test
    @DisplayName("Should extract genuine Aadhaar text without hallucinating missing fields")
    void testExtractGenuineAadhaarText() {
        String rawAadhaarContent = "GOVERNMENT OF INDIA\n" +
                "Unique Identification Authority of India\n" +
                "Enrollment No: 1234/56789/01234\n" +
                "To\n" +
                "Aditya Kumar\n" +
                "S/O Ramesh Kumar\n" +
                "DOB: 14/08/2005\n" +
                "Male\n" +
                "1234 5678 9012\n" +
                "Mera Aadhaar, Meri Pehchan";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aditya_aadhaar.txt",
                "text/plain",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                null // no profile check
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());
        assertNotNull(response.getFields());

        // Document Number must match genuine value (masked in Aadhaar)
        DocumentFieldExtraction docNumField = response.getFields().get("documentNumber");
        assertNotNull(docNumField);
        assertEquals("XXXX-XXXX-9012", docNumField.getValue());
        assertEquals("FOUND", docNumField.getStatus());

        // DOB must match genuine value
        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2005-08-14", dobField.getValue());
        assertEquals("FOUND", dobField.getStatus());

        // Gender
        DocumentFieldExtraction genderField = response.getFields().get("gender");
        assertNotNull(genderField);
        assertEquals("MALE", genderField.getValue());
    }

    @Test
    @DisplayName("Should detect IDENTITY_MISMATCH when Aadhaar holder name differs from authenticated citizen")
    void testIdentityMismatchDifferentHolder() {
        // Authenticated citizen: Lathika.K, DOB: 2007-03-22
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Uploaded Aadhaar text: Aditya Kumar, DOB: 14/08/2005
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "Name: Aditya Kumar\n" +
                "DOB: 14/08/2005\n" +
                "Gender: Male\n" +
                "9876 5432 1098";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_card.txt",
                "text/plain",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus());
        assertFalse(idResult.isNameMatch(), "Name match must be false");
        assertFalse(idResult.isOverallMatch(), "Overall match must be false");
        assertNotNull(idResult.getFailureReason());
        assertTrue(idResult.getFailureReason().contains("do not match") || idResult.getFailureReason().contains("does not match"));
        assertTrue(idResult.getDetails().stream().anyMatch(d -> d.contains("Aditya Kumar")));
    }

    @Test
    @DisplayName("Should verify identity match with name normalization (e.g. initials 'Lathika.K' vs 'Lathika Kumar')")
    void testIdentityMatchWithInitialsNormalization() {
        // Profile: Lathika.K
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Aadhaar: Name: Lathika Kumar, DOB: 22/03/2007
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "Name: Lathika Kumar\n" +
                "DOB: 22/03/2007\n" +
                "Gender: Female\n" +
                "5482 9102 4589";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lathika_aadhaar.txt",
                "text/plain",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus());
        assertTrue(idResult.isNameMatch(), "Name with initial expansion should match");
        assertTrue(idResult.isDobMatch(), "DOB 22/03/2007 should match 2007-03-22");
        assertTrue(idResult.isOverallMatch(), "Overall match must be true");
    }

    @Test
    @DisplayName("Should return null and NOT_FOUND for fields not present in document, preventing fabrication")
    void testMissingFieldsReturnNullAndNotFound() {
        // Text without document number or DOB
        String text = "Sample Department Document\n" +
                "Applicant was present for verification\n" +
                "Revenue Authority";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "generic.txt",
                "text/plain",
                text.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "GENERAL",
                "General",
                null
        );

        assertNotNull(response);
        DocumentFieldExtraction docNumField = response.getFields().get("documentNumber");
        assertNotNull(docNumField);
        assertNull(docNumField.getValue(), "Document number must be null if absent from document");
        assertEquals("NOT_FOUND", docNumField.getStatus());

        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertNull(dobField.getValue(), "DOB must be null if absent from document");
        assertEquals("NOT_FOUND", dobField.getStatus());
    }

    @Test
    @DisplayName("Vault service should reject mismatched documents and prevent cross-scheme reuse")
    void testVaultServiceRejectsMismatchAndPreventsReuse() {
        // Authenticated citizen: Lathika.K
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(profile));

        when(documentStorageService.storeVaultDocument(any(), any(), any()))
                .thenReturn("gridfs-ref-456");

        when(vaultDocumentRepository.save(any(CitizenVaultDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Upload Aditya's Aadhaar
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "Name: Aditya Kumar\n" +
                "DOB: 14/08/2005\n" +
                "9876 5432 1098";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aditya_aadhaar.pdf",
                "application/pdf",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        CitizenVaultDocument savedDoc = vaultDocumentService.uploadVaultDocument(
                "user-lathika",
                "AADHAAR",
                "Identity Proof",
                "Aadhaar Card",
                "UIDAI",
                "No Expiration",
                null,
                null,
                "Manual Upload",
                file
        );

        assertNotNull(savedDoc);
        assertEquals(DocumentVerificationStatus.REJECTED, savedDoc.getVerificationStatus());
        assertEquals(DetailedDocumentStatus.AI_REJECTED, savedDoc.getDetailedStatus());
        assertEquals("MISMATCH", savedDoc.getIdentityMatchStatus());
        assertNotNull(savedDoc.getRejectionReason());
        assertTrue(savedDoc.getRejectionReason().contains("holder details do not match"));

        // Now verify cross-scheme reuse skips this rejected mismatched document:
        when(vaultDocumentRepository.findByUserIdOrderByUploadedAtDesc("user-lathika"))
                .thenReturn(java.util.List.of(savedDoc));

        Optional<CitizenVaultDocument> reusableDoc = vaultDocumentService.findMatchingVaultDocument(
                "user-lathika",
                "AADHAAR",
                "Aadhaar Card",
                Collections.emptyList()
        );
        assertFalse(reusableDoc.isPresent(), "Mismatched / rejected document must NEVER be reused across schemes");
    }

    @Test
    @DisplayName("Should detect MISMATCH when document has same applicant name but different DOB")
    void testIdentityMismatch_SameNameDifferentDob() {
        // Authenticated citizen: Lathika Kumar, DOB: 2007-03-22
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Uploaded Aadhaar text: Same name Lathika Kumar, but different DOB: 14/08/2005
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "Name: Lathika Kumar\n" +
                "DOB: 14/08/2005\n" +
                "Gender: Female\n" +
                "5482 9102 4589";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lathika_different_dob_aadhaar.txt",
                "text/plain",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus(), "Contradictory DOB must result in MISMATCH");
        assertTrue(idResult.isNameMatch(), "Name matches, so isNameMatch must be true");
        assertFalse(idResult.isDobMatch(), "DOB differs, so isDobMatch must be false");
        assertFalse(idResult.isOverallMatch(), "Overall match must be false");
        assertNotNull(idResult.getFailureReason());
        assertTrue(idResult.getDetails().stream().anyMatch(d -> d.contains("DOB mismatch")),
                "Details must explicitly explain the DOB mismatch");
    }

    @Test
    @DisplayName("Should verify MATCH when genuine document matches both name and DOB (DD/MM/YYYY vs ISO)")
    void testIdentityMatch_MatchingNameAndMatchingDobNormalized() {
        // Authenticated citizen: Lathika Kumar, DOB: 2007-03-22
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Uploaded Aadhaar text: Name: Lathika Kumar, DOB: 22/03/2007
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "Name: Lathika Kumar\n" +
                "DOB: 22/03/2007\n" +
                "Gender: Female\n" +
                "5482 9102 4589";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lathika_correct_aadhaar.txt",
                "text/plain",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus());
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isDobMatch(), "22/03/2007 must normalize and match 2007-03-22");
        assertTrue(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Extraction pipeline must never receive applicant DOB as prompt context")
    void testExtractionNeverReceivesApplicantProfileAsInput() {
        // Verify that extraction prompt method only uses document code and hint, with zero applicant state
        StructuredDocumentExtractionResponse resp = extractionService.performExtraction(
                "Sample Government Document\nNo DOB here".getBytes(StandardCharsets.UTF_8),
                "test.txt",
                "text/plain",
                "AADHAAR",
                "Identity Proof"
        );
        assertNotNull(resp);
        // Field dateOfBirth must be NOT_FOUND, never hallucinated from any applicant profile
        DocumentFieldExtraction dobField = resp.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertNull(dobField.getValue());
        assertEquals("NOT_FOUND", dobField.getStatus());
    }

    @Test
    @DisplayName("Income Certificate should verify based on holder name only without requiring or checking DOB")
    void testIncomeCertificateNameOnlyMatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Income Certificate with name Lathika Kumar and NO DOB in document
        String rawIncomeContent = "GOVERNMENT OF UTTAR PRADESH\n" +
                "Revenue Department - Income Certificate\n" +
                "Certificate No: UP/INC/2026/009182\n" +
                "Name: Lathika Kumar\n" +
                "Annual Family Income: Rs. 1,50,000\n" +
                "Competent Authority: Tehsildar";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "income_certificate.txt",
                "text/plain",
                rawIncomeContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "INCOME_CERTIFICATE",
                "Financial Proof",
                "user-lathika"
        );

        assertNotNull(response);
        assertEquals("INCOME_CERTIFICATE", response.getDocumentType());
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika Kumar", nameField.getValue());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Income certificate should match on name alone");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isOverallMatch(), "Overall match must be true even without DOB");
        assertTrue(idResult.getDetails().stream().anyMatch(d -> d.contains("name-only") || d.contains("Name match")));
    }

    @Test
    @DisplayName("Income Certificate should detect MISMATCH when holder name differs")
    void testIncomeCertificateNameMismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Income Certificate belonging to different person
        String rawIncomeContent = "GOVERNMENT OF UTTAR PRADESH\n" +
                "Revenue Department - Income Certificate\n" +
                "Certificate No: UP/INC/2026/009182\n" +
                "Name: Ramesh Sharma\n" +
                "Annual Family Income: Rs. 1,50,000\n" +
                "Competent Authority: Tehsildar";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "income_diff.txt",
                "text/plain",
                rawIncomeContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "INCOME_CERTIFICATE",
                "Financial Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus());
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Caste Certificate should verify based on holder name only without requiring or checking DOB")
    void testCasteCertificateNameOnlyMatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Caste Certificate with name Lathika Kumar and NO DOB
        String rawCasteContent = "DEPARTMENT OF SOCIAL WELFARE\n" +
                "Community and Caste Certificate\n" +
                "Certificate No: UP/CST/2025/11245\n" +
                "Name: Lathika Kumar\n" +
                "Category: OBC\n" +
                "Competent Authority: District Magistrate";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "caste_certificate.txt",
                "text/plain",
                rawCasteContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "CASTE_CERTIFICATE",
                "Category Proof",
                "user-lathika"
        );

        assertNotNull(response);
        assertEquals("CASTE_CERTIFICATE", response.getDocumentType());
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika Kumar", nameField.getValue());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Caste certificate should match on name alone");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isOverallMatch(), "Overall match must be true even without DOB");
    }

    @Test
    @DisplayName("Caste Certificate should detect MISMATCH when holder name differs")
    void testCasteCertificateNameMismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Caste Certificate belonging to different person
        String rawCasteContent = "DEPARTMENT OF SOCIAL WELFARE\n" +
                "Community and Caste Certificate\n" +
                "Certificate No: UP/CST/2025/11245\n" +
                "Name: Sunil Verma\n" +
                "Category: SC\n" +
                "Competent Authority: District Magistrate";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "caste_diff.txt",
                "text/plain",
                rawCasteContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "CASTE_CERTIFICATE",
                "Category Proof",
                "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus());
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Should reject font glyph artifact 'bTTT', correctly extract 'Lathika' and Aadhaar number, and verify MATCH with profile 'Lathika.K'")
    void testExtractAadhaarWithCorruptedDevanagariGlyphArtifact_Rejects_bTTT_And_Extracts_Lathika() {
        // Authenticated citizen profile: Lathika.K, DOB: 2007-03-22
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Aadhaar document with regional Devanagari line stripped as ASCII glyph artifact 'bTTT'
        String rawAadhaarContent = "Unique Identification Authority of India\n" +
                "To\n" +
                "Lathika\n" +
                "bTTT\n" +
                "DOB: 22/03/2007\n" +
                "Gender: Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lathika_aadhaar_card.pdf",
                "application/pdf",
                rawAadhaarContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file,
                "AADHAAR",
                "Identity Proof",
                "user-lathika"
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());
        assertNotNull(response.getFields());

        // 1. Holder Name MUST be extracted as genuine name "Lathika", NEVER "bTTT"
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue(), "Holder name must be Lathika, not corrupted glyph bTTT");
        assertEquals("FOUND", nameField.getStatus());

        // 2. Date of birth must be correctly normalized to 2007-03-22
        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2007-03-22", dobField.getValue());
        assertEquals("FOUND", dobField.getStatus());

        // 3. Aadhaar Number must be correctly extracted
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue());
        assertEquals("FOUND", numField.getStatus());

        // 4. Gender
        DocumentFieldExtraction genderField = response.getFields().get("gender");
        assertNotNull(genderField);
        assertEquals("FEMALE", genderField.getValue());

        // 5. Identity Verification MUST be a MATCH against authenticated applicant "Lathika.K"
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Identity verification must be MATCH for Lathika vs Lathika.K");
        assertTrue(idResult.isNameMatch(), "Name match must be true");
        assertTrue(idResult.isDobMatch(), "DOB match must be true");
        assertTrue(idResult.isOverallMatch(), "Overall match must be true");
    }

    @Test
    @DisplayName("Should extract Aadhaar number across all valid formats: labeled, multi-spaced, hyphenated, and contiguous")
    void testExtractAadhaarNumberFormattingVariants() {
        String[] variants = {
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n5826 1294 0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n5826  1294  0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n5826\u00A01294\u00A00244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n5826-1294-0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n5826.1294.0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n582612940244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\nAadhaar Number: 5826 1294 0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\nAadhaar No: 5826 1294 0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\nUID: 5826 1294 0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\nXXXX-XXXX-0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\nXXXX XXXX 0244",
                "Unique Identification Authority of India\nLathika\nDOB: 22/03/2007\n**** **** 0244"
        };

        for (String content : variants) {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)
            );
            StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                    file, "AADHAAR", "Identity Proof", null
            );
            assertNotNull(response);
            DocumentFieldExtraction numField = response.getFields().get("documentNumber");
            assertNotNull(numField, "Document number field must exist for variant: " + content);
            assertEquals("XXXX-XXXX-0244", numField.getValue(), "Failed to extract Aadhaar number for variant: " + content);
            assertEquals("FOUND", numField.getStatus());
        }
    }

    @Test
    @DisplayName("Should reject document and return NOT_FOUND when only corrupted glyph noise is present (Zero Profile Leaking)")
    void testRejectDocumentWhenOnlyGlyphNoisePresent_ZeroProfileLeaking() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Document has ONLY glyph noise "bTTT", NO valid human name anywhere
        String rawNoNameContent = "Unique Identification Authority of India\n" +
                "bTTT\n" +
                "DOB: 22/03/2007\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.txt", "text/plain", rawNoNameContent.getBytes(StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertNull(nameField.getValue(), "Holder name MUST be null when only glyph noise exists - NEVER fall back to profile!");
        assertEquals("NOT_FOUND", nameField.getStatus());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertTrue("UNCERTAIN".equals(idResult.getStatus()) || "MISMATCH".equals(idResult.getStatus()),
                "Absence of valid holder name must yield UNCERTAIN or MISMATCH");
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("isValidName must reject column-merge corruption 'IoGfn odviae ramenAtu thority Cuddalore'")
    void testIsValidName_RejectsColumnMergeCorruption() {
        // This string is produced by PDFBox when left-column address text bleeds into the name area
        // across multi-column UIDAI letter layouts with embedded font subset encodings.
        // All words have vowels and no 3-in-a-row characters — but they contain:
        //   - intra-word mixed casing: IoGfn, ramenAtu
        //   - known geographic stop-word: Cuddalore
        //   - 5+ tokens (implausible for a person name)
        String corrupt = "IoGfn odviae ramenAtu thority Cuddalore";
        // Access isValidName indirectly through the extraction pipeline using a document
        // that only has this corrupt string as the only text near the DOB.
        String rawContent = "Unique Identification Authority of India\n" +
                "IoGfn odviae ramenAtu thority Cuddalore\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt_column.txt", "text/plain",
                rawContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        // The corrupt string MUST NOT be returned as holderName
        assertNotEquals("IoGfn odviae ramenAtu thority Cuddalore", nameField.getValue(),
                "Column-merge corruption must be rejected, not returned as holder name");
    }

    @Test
    @DisplayName("extractAadhaarNumber must prefer 5826 1294 0244 (near DOB) over enrollment-line number ending in 9464")
    void testExtractAadhaarNumber_PrefersCardNumberOverEnrollmentNumber() {
        // Simulates a UIDAI letter where enrollment number (containing 9464) appears BEFORE the card
        // cutout that contains the actual Aadhaar number 5826 1294 0244 near the DOB line.
        String rawContent = "Unique Identification Authority of India\n" +
                "Enrollment No: 1234/56789/09464\n" +   // <- enrollment, contains 9464 but has /
                "To\n" +
                "Lathika\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n" +                    // <- actual card Aadhaar number
                "Mera Aadhaar, Meri Pehchan";

        MockMultipartFile file = new MockMultipartFile(
                "file", "lathika_aadhaar_full.txt", "text/plain",
                rawContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(),
                "Must extract card Aadhaar (0244), NOT enrollment number (9464)");
        assertEquals("FOUND", numField.getStatus());
    }

    @Test
    @DisplayName("Full flow: real-world UIDAI letter with enrollment prefix and column-merge text extracts Lathika/0244 and returns MATCH")
    void testFullFlow_RealWorldUidaiLetter_LathikaK_Match() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();

        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // Simulated real-world UIDAI e-letter text (after PDFBox extraction with sortByPosition=true)
        // The enrollment line has slashes -> excluded from Aadhaar candidates
        // The name "IoGfn odviae ramenAtu thority Cuddalore" appears due to column-merge -> rejected
        // "Lathika" appears after "To" -> accepted as name
        // "5826 1294 0244" appears after DOB/Gender -> preferred over any enrollment digits
        String rawContent = "Unique Identification Authority of India\n" +
                "Government of India\n" +
                "Enrollment No: 1234/56789/09464\n" +
                "To\n" +
                "Lathika\n" +
                "IoGfn odviae ramenAtu thority Cuddalore\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n" +
                "Mera Aadhaar, Meri Pehchan";

        MockMultipartFile file = new MockMultipartFile(
                "file", "lathika_uidai_letter.txt", "text/plain",
                rawContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());

        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue(), "Holder name must be Lathika");
        assertEquals("FOUND", nameField.getStatus());

        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2007-03-22", dobField.getValue());

        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(),
                "Must return 0244 (card number), not 9464 (enrollment suffix)");

        DocumentFieldExtraction genderField = response.getFields().get("gender");
        assertNotNull(genderField);
        assertEquals("FEMALE", genderField.getValue());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Lathika must MATCH profile Lathika.K");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isDobMatch());
        assertTrue(idResult.isOverallMatch());
    }

    // =========================================================================
    // ITERATION 3 REGRESSION TESTS
    // Covering the exact failure variants from the real UIDAI e-letter PDF:
    //   holderName = "IoGfn odviae ramenAtu th ority Cuddalore"  (with space in "th ority")
    //   documentNumber = XXXX-XXXX-9464 instead of XXXX-XXXX-0244
    // =========================================================================

    @Test
    @DisplayName("Test 1 – Full UIDAI letter with admin header + Cuddalore extracts Lathika correctly")
    void test1_UidaiLetterWithAdminHeader_ExtractsLathikaAndCorrectAadhaar() {
        // Simulates text produced by PDFBox from actual UIDAI e-Aadhaar letter layout:
        //  - Header region: "Government of India" / "Unique Identification Authority..."
        //  - Column-merge artifact: "IoGfn odviae ramenAtu th ority Cuddalore"
        //  - Actual identity region: "To\nLathika\nDOB: 22/03/2007\nFemale"
        //  - Card number: "5826 1294 0244"
        String rawText = "Government of India\n" +
                "Unique Identification Authority of India\n" +
                "Enrollment No: 1234/56789/09464\n" +
                "IoGfn odviae ramenAtu th ority Cuddalore\n" +
                "To\n" +
                "Lathika\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n" +
                "Mera Aadhaar, Meri Pehchan";

        MockMultipartFile file = new MockMultipartFile(
                "file", "lathika_uidai_full_layout.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());

        // holderName MUST be "Lathika", NOT the column-merge corruption
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue(),
                "Holder name must be Lathika, NOT the admin/header corruption");
        assertEquals("FOUND", nameField.getStatus());

        // DOB
        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2007-03-22", dobField.getValue());

        // Aadhaar number: 0244 from card section, NOT 9464 from enrollment line
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(),
                "Must use card Aadhaar (0244), not enrollment suffix (9464)");
    }

    @Test
    @DisplayName("Test 2 – 'IoGfn odviae ramenAtu th ority Cuddalore' (space-split variant) MUST NOT be holderName")
    void test2_SpaceSplitCorruption_th_ority_MustBeRejected() {
        // This is the exact corruption string from the real PDF with space in "th ority"
        // (PDFBox splits "authority" glyph into two tokens "th" and "ority")
        String rawText = "Unique Identification Authority of India\n" +
                "IoGfn odviae ramenAtu th ority Cuddalore\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "space_split_corrupt.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);

        // The corrupt string with space-split MUST be rejected
        assertNotEquals("IoGfn odviae ramenAtu th ority Cuddalore", nameField.getValue(),
                "Column-merge 'th ority' variant must be rejected");
        // "th ority" (after other tokens removed) must also be rejected
        assertNotEquals("th ority", nameField.getValue(),
                "'th ority' fragment must not be accepted as a person name");
        // "th" alone must also be rejected
        assertNotEquals("th", nameField.getValue(),
                "'th' consonant fragment must not be a holder name");
    }

    @Test
    @DisplayName("Test 3 – 'IoGfn' specifically caught by updated intra-word mixed-case pattern (oG transition)")
    void test3_IoGfn_CaughtByUpdatedMixedCasePattern() {
        // Before fix: PATTERN_INTRAWORD_MIXED_CASE required 2+ lowercase before uppercase
        //             → "IoGfn" had only 1 lowercase (o) before G → NOT caught
        // After fix:  Pattern is [a-z][A-Z] → oG matches → "IoGfn" IS caught
        String rawText = "Unique Identification Authority of India\n" +
                "IoGfn\n" +                 // <-- only this candidate near DOB
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "iogfn_only.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertNull(nameField.getValue(),
                "IoGfn must be rejected by intra-word mixed-case detection (oG transition)");
        assertEquals("NOT_FOUND", nameField.getStatus());
    }

    @Test
    @DisplayName("Test 6 – Multiple 12-digit Aadhaar-like sequences: semantic proximity selects correct card number")
    void test6_MultipleAadhaarLikeSequences_SelectsCardProximityToDoB() {
        // Three 12-digit sequences appear:
        //  - Line 2: 1111 2222 9464 (near top, far from DOB)
        //  - Line 7: 5826 1294 0244 (below DOB/Gender — card cutout area, highest score)
        //  - Line 9: 9999 8888 7777 (after card area)
        // Only "5826 1294 0244" should be selected.
        String rawText = "Government of India\n" +
                "1111 2222 9464\n" +          // far from DOB — low score
                "To\n" +
                "Lathika\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n" +          // 1 line after DOB — high score
                "Mera Aadhaar\n" +
                "9999 8888 7777\n";           // further down

        MockMultipartFile file = new MockMultipartFile(
                "file", "multi_aadhaar_candidates.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(),
                "Must select 0244 (closest to DOB after), NOT 9464 (far from DOB) or 7777");
    }

    @Test
    @DisplayName("Test 7 – Enrollment slash-format excluded; card number near DOB selected")
    void test7_EnrollmentSlashFormat_ExcludedFromCandidates() {
        // Enrollment No in slash-format: 1234/56789/09464
        // Card Aadhaar: 5826 1294 0244 (near DOB)
        // Expected: XXXX-XXXX-0244 (enrollment excluded due to slash on same line)
        String rawText = "Unique Identification Authority of India\n" +
                "Enrollment No: 1234/56789/09464\n" +
                "To\n" +
                "Lathika\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "enrollment_slash_test.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", null
        );

        assertNotNull(response);
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(),
                "Enrollment slash-line must be excluded; 0244 must be selected");
    }

    @Test
    @DisplayName("Test 8 – Document with ONLY corrupted glyph candidates returns holderName=null (zero profile fallback)")
    void test8_OnlyCorruptedGlyphCandidates_ReturnsNullWithNoProfileLeaking() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(applicantProfile));

        // NO valid human name anywhere — only corruption artifacts
        String rawText = "Unique Identification Authority of India\n" +
                "IoGfn\n" +
                "odviae\n" +
                "ramenAtu\n" +
                "th ority\n" +
                "Cuddalore 607001\n" +
                "DOB: 22/03/2007\n" +
                "Female\n" +
                "5826 1294 0244\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "all_corrupt_candidates.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        // MUST be null — never substitute profile name
        assertNull(nameField.getValue(),
                "holderName MUST be null when only corruption artifacts exist. Profile name must NEVER be substituted.");
        assertEquals("NOT_FOUND", nameField.getStatus());

        // Identity must be MISMATCH (no valid name → can't confirm identity)
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertTrue("UNCERTAIN".equals(idResult.getStatus()) || "MISMATCH".equals(idResult.getStatus()));
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Test 9a – Income certificate name-only match: DOB not required")
    void test9a_IncomeCertificate_NameOnlyVerification_StillPasses() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(profile));

        String rawText = "GOVERNMENT OF TAMIL NADU\n" +
                "Revenue Department - Income Certificate\n" +
                "Certificate No: TN/INC/2026/003344\n" +
                "Name: Lathika Kumar\n" +
                "Annual Family Income: Rs. 2,00,000\n" +
                "Competent Authority: Tehsildar";

        MockMultipartFile file = new MockMultipartFile(
                "file", "lathika_income_cert.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika"
        );

        assertNotNull(response);
        assertEquals("INCOME_CERTIFICATE", response.getDocumentType());
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika Kumar", nameField.getValue());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Income cert must match on name alone");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isOverallMatch());
        assertTrue(idResult.getDetails().stream()
                .anyMatch(d -> d.contains("name-only") || d.contains("Name match")));
    }

    @Test
    @DisplayName("Test 9b – Caste certificate name-only match: different person → MISMATCH")
    void test9b_CasteCertificate_WrongHolder_Mismatch() {
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika Kumar")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika"))
                .thenReturn(Optional.of(profile));

        String rawText = "DEPARTMENT OF SOCIAL WELFARE\n" +
                "Caste Certificate\n" +
                "Certificate No: TN/CST/2025/99921\n" +
                "Name: Priya Sharma\n" +
                "Category: OBC\n" +
                "Competent Authority: District Magistrate";

        MockMultipartFile file = new MockMultipartFile(
                "file", "wrong_caste_cert.txt", "text/plain",
                rawText.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "CASTE_CERTIFICATE", "Category Proof", "user-lathika"
        );

        assertNotNull(response);
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus());
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Test Real PDF - Inspect extraction on Lathika Originals-7.pdf")
    void testRealPdfExtraction() throws Exception {
        java.io.File pdfFile = new java.io.File("C:/Users/hp5cd/Downloads/Lathika Originals-7.pdf");
        if (!pdfFile.exists()) {
            System.out.println("Real PDF not found, skipping");
            return;
        }
        byte[] pdfBytes = java.nio.file.Files.readAllBytes(pdfFile.toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "Lathika Originals-7.pdf", "application/pdf", pdfBytes
        );

        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(profile));

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());
        assertNotNull(response.getFields());

        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue(), "Extracted name must be 'Lathika' from genuine Aadhaar text");
        assertEquals("FOUND", nameField.getStatus());

        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(), "Extracted number must be 0244, not enrollment 9464");
        assertEquals("FOUND", numField.getStatus());

        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2007-03-22", dobField.getValue());
        assertEquals("FOUND", dobField.getStatus());

        DocumentFieldExtraction genderField = response.getFields().get("gender");
        assertNotNull(genderField);
        assertEquals("FEMALE", genderField.getValue());
        assertEquals("FOUND", genderField.getStatus());

        DocumentFieldExtraction authField = response.getFields().get("issuingAuthority");
        assertNotNull(authField);
        assertEquals("UIDAI", authField.getValue());
        assertEquals("FOUND", authField.getStatus());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Identity verification must MATCH against applicant Lathika.K");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isDobMatch());
        assertTrue(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Test Real Image - Visual OCR extraction on Screenshot_20260919-091359.png (DigiLocker Aadhaar)")
    void testRealAadhaarImageExtraction() throws Exception {
        java.io.File imgFile = new java.io.File("E:/SCHEMEBRIDGE/test-documents/Screenshot_20260919-091359.png");
        if (!imgFile.exists()) {
            System.out.println("Real image not found, skipping");
            return;
        }
        byte[] imgBytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "Screenshot_20260919-091359.png", "image/png", imgBytes
        );

        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(profile));

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType(), "DigiLocker image must be classified as AADHAAR, not generic GOVERNMENT_DOCUMENT");
        assertNotNull(response.getFields());

        // 1. Holder Name
        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika K", nameField.getValue(), "Visual OCR must extract holder name 'Lathika K' directly above DOB");
        assertEquals("FOUND", nameField.getStatus());
        assertEquals("OCR_VISUAL", nameField.getSource());

        // 2. Document Number (masked)
        DocumentFieldExtraction numField = response.getFields().get("documentNumber");
        assertNotNull(numField);
        assertEquals("XXXX-XXXX-0244", numField.getValue(), "Extracted Aadhaar number must end in 0244 from 'xxxxxxxx0244'");
        assertEquals("FOUND", numField.getStatus());
        assertEquals("OCR_VISUAL", numField.getSource());

        // 3. Date of Birth
        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertEquals("2007-03-22", dobField.getValue(), "Extracted DOB must be normalized to '2007-03-22'");
        assertEquals("FOUND", dobField.getStatus());
        assertEquals("OCR_VISUAL", dobField.getSource());

        // 4. Gender
        DocumentFieldExtraction genderField = response.getFields().get("gender");
        assertNotNull(genderField);
        assertEquals("FEMALE", genderField.getValue());
        assertEquals("FOUND", genderField.getStatus());

        // 5. Identity Verification
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus(), "Identity verification must MATCH against applicant Lathika.K");
        assertTrue(idResult.isNameMatch());
        assertTrue(idResult.isDobMatch());
        assertTrue(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Test Real Image - Mismatch when Aditya uploads Lathika's Aadhaar image")
    void testRealAadhaarImage_MismatchWhenUploadedByOtherCitizen() throws Exception {
        java.io.File imgFile = new java.io.File("E:/SCHEMEBRIDGE/test-documents/Screenshot_20260919-091359.png");
        if (!imgFile.exists()) {
            return;
        }
        byte[] imgBytes = java.nio.file.Files.readAllBytes(imgFile.toPath());
        MockMultipartFile file = new MockMultipartFile(
                "file", "Screenshot_20260919-091359.png", "image/png", imgBytes
        );

        // Different authenticated applicant: Aditya Kumar, born 2005-08-14
        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-aditya")
                .displayName("Aditya Kumar")
                .dob(LocalDate.of(2005, 8, 14))
                .gender("Male")
                .build();
        when(citizenProfileRepository.findByUserId("user-aditya")).thenReturn(Optional.of(profile));

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-aditya"
        );

        assertNotNull(response);
        assertEquals("AADHAAR", response.getDocumentType());
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MISMATCH", idResult.getStatus(), "Uploading another citizen's document must result in MISMATCH");
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isDobMatch());
        assertFalse(idResult.isOverallMatch());
    }

    @Test
    @DisplayName("Test Aadhaar Number formatting variations: multiple spaces, NBSP, hyphens, dots, masked")
    void testAadhaarNumberFormattingVariations() {
        // Multiple spaces
        String multiSpace = "Government of India\nAadhaar\nName\nLathika K\nDOB: 22/03/2007\nFemale\n5826   1294   0244";
        var res1 = extractionService.performGroundedDeterministicExtraction(multiSpace.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res1.getFields().get("documentNumber").getValue());

        // NBSP
        String nbsp = "Government of India\nAadhaar\nName\nLathika K\nDOB: 22/03/2007\nFemale\n5826\u00A01294\u00A00244";
        var res2 = extractionService.performGroundedDeterministicExtraction(nbsp.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res2.getFields().get("documentNumber").getValue());

        // Hyphens
        String hyphens = "Government of India\nAadhaar\nName\nLathika K\nDOB: 22/03/2007\nFemale\n5826-1294-0244";
        var res3 = extractionService.performGroundedDeterministicExtraction(hyphens.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res3.getFields().get("documentNumber").getValue());

        // Dots
        String dots = "Government of India\nAadhaar\nName\nLathika K\nDOB: 22/03/2007\nFemale\n5826.1294.0244";
        var res4 = extractionService.performGroundedDeterministicExtraction(dots.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res4.getFields().get("documentNumber").getValue());

        // Masked contiguous (DigiLocker): xxxxxxxx0244
        String maskedContig = "AADHAAR\nLathika K\n2007-03-22\nFemale\nxxxxxxxx0244";
        var res5 = extractionService.performGroundedDeterministicExtraction(maskedContig.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res5.getFields().get("documentNumber").getValue());

        // Masked asterisks: **** **** 0244
        String maskedAst = "AADHAAR\nLathika K\n2007-03-22\nFemale\n**** **** 0244";
        var res6 = extractionService.performGroundedDeterministicExtraction(maskedAst.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res6.getFields().get("documentNumber").getValue());

        // Masked standard: XXXX-XXXX-0244
        String maskedStd = "AADHAAR\nLathika K\n2007-03-22\nFemale\nXXXX-XXXX-0244";
        var res7 = extractionService.performGroundedDeterministicExtraction(maskedStd.getBytes(StandardCharsets.UTF_8), "doc.txt", "text/plain", "AADHAAR", null);
        assertEquals("XXXX-XXXX-0244", res7.getFields().get("documentNumber").getValue());
    }

    @Test
    @DisplayName("Test Profile Isolation: Missing document fields return null and are NEVER populated from applicant profile")
    void testProfileIsolation_NeverPopulateFromProfile() {
        // Document has NO readable name and NO readable DOB
        String unreadableDoc = "GOVERNMENT OF INDIA\nUnique Identification Authority of India\nEnrollment No: 1234/56789/01234\nHelpdesk: 1947";
        MockMultipartFile file = new MockMultipartFile(
                "file", "blank_aadhaar.txt", "text/plain", unreadableDoc.getBytes(StandardCharsets.UTF_8)
        );

        CitizenProfile profile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(profile));

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(
                file, "AADHAAR", "Identity Proof", "user-lathika"
        );

        assertNotNull(response);
        // CRITICAL: Document holderName must be null / NOT_FOUND, NEVER "Lathika.K"
        assertNull(response.getFields().get("holderName").getValue(), "Holder name must remain null when not in document");
        assertEquals("NOT_FOUND", response.getFields().get("holderName").getStatus());

        // CRITICAL: Document dateOfBirth must be null / NOT_FOUND, NEVER "2007-03-22"
        assertNull(response.getFields().get("dateOfBirth").getValue(), "DOB must remain null when not in document");
        assertEquals("NOT_FOUND", response.getFields().get("dateOfBirth").getStatus());

        // Identity verification must NOT match
        assertFalse(response.getIdentityVerification().isOverallMatch(), "Cannot claim MATCH when document fields are missing");
        assertTrue("UNCERTAIN".equals(response.getIdentityVerification().getStatus()) || "MISMATCH".equals(response.getIdentityVerification().getStatus()));
    }

    @Test
    @DisplayName("Test Name Consistency: Initial and punctuation handling")
    void testNameConsistencyCases() {
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Lathika K"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Lathika"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Lathika Kumar"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "K. Lathika"));
        assertFalse(extractionService.isNameConsistent("Lathika.K", "Aditya Kumar"));
        assertFalse(extractionService.isNameConsistent("Lathika.K", "Ramesh Kumar"));
        assertFalse(extractionService.isNameConsistent("Lathika.K", "Nadu"));
        assertTrue(extractionService.isNameConsistent("Lathika.K", "Selvi Lathika"));
    }

    @Test
    @DisplayName("TEST 1: Income Certificate with 'This is to certify that Selvi Lathika daughter of Thiru Kumar...'")
    void testIncomeCertificate_CertifyHolder_Match() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Tamil Nadu e-District\n" +
                "Income Certificate\n" +
                "Certificate No: TN-4202403185459\n" +
                "Date: 22-03-2024\n" +
                "This is to certify that Selvi Lathika daughter of Thiru Kumar Late residing at Door No. 428 Kozhal street\n" +
                "His /Her family annual income based on details furnished is RS. 72000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "income_cert.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("INCOME_CERTIFICATE", response.getDocumentType());
        assertEquals("Income Certificate", response.getDocumentName());

        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue());
        assertEquals("FOUND", nameField.getStatus());

        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertNull(dobField.getValue());
        assertEquals("NOT_FOUND", dobField.getStatus());

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus());
        assertTrue(idResult.isNameMatch());
        assertNull(idResult.getDobMatch(), "DOB match must be null for name-only documents");
        assertTrue(idResult.isOverallMatch());
        assertTrue(idResult.getDetails().stream().anyMatch(d -> d.contains("Lathika") && d.contains("Lathika.K")));
        assertFalse(idResult.getDetails().stream().anyMatch(d -> d.contains("Date of birth not verified")));
    }

    @Test
    @DisplayName("TEST 1B: Real Income Certificate PDF (Lathika Originals-6.pdf) holder extraction and verification")
    void testRealIncomeCertificate_LathikaOriginals6() throws Exception {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .gender("Female")
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        File pdfFile = new File("E:/SCHEMEBRIDGE/test-documents/Lathika Originals-6.pdf");
        assertTrue(pdfFile.exists(), "Lathika Originals-6.pdf must exist in test-documents");

        byte[] pdfBytes = java.nio.file.Files.readAllBytes(pdfFile.toPath());
        MockMultipartFile file = new MockMultipartFile("file", "Lathika Originals-6.pdf", "application/pdf", pdfBytes);

        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("INCOME_CERTIFICATE", response.getDocumentType());
        assertEquals("Income Certificate", response.getDocumentName());

        DocumentFieldExtraction nameField = response.getFields().get("holderName");
        assertNotNull(nameField);
        assertEquals("Lathika", nameField.getValue());
        assertNotEquals("Nadu. This Lual", nameField.getValue(), "Holder name must never be OCR/address fragment");

        DocumentFieldExtraction dobField = response.getFields().get("dateOfBirth");
        assertNotNull(dobField);
        assertNull(dobField.getValue(), "DOB must be null for Income Certificate");

        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertNotNull(idResult);
        assertEquals("MATCH", idResult.getStatus());
        assertTrue(idResult.isNameMatch());
        assertNull(idResult.getDobMatch());
        assertTrue(idResult.isOverallMatch());
        assertFalse(idResult.getDetails().stream().anyMatch(d -> d.contains("Date of birth not verified")));
    }

    @Test
    @DisplayName("TEST 2: Income Certificate where family table also contains Lathika")
    void testIncomeCertificate_TableAlsoContainsLathika() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Income Certificate\n" +
                "Certificate No: TN-4202403185459\n" +
                "This is to certify that Selvi Lathika daughter of Thiru Kumar Late\n" +
                "Name of the family Member\n" +
                "MangaiyarkArasi Mother\n" +
                "Lathika Self\n" +
                "Total Annual Income is RS. 72000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "income_table.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
        assertTrue(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("TEST 3: Income Certificate for Priya uploaded by Lathika returns MISMATCH")
    void testIncomeCertificate_Priya_Mismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Income Certificate\n" +
                "Certificate No: TN-4202403999999\n" +
                "This is to certify that Selvi Priya daughter of Thiru Murugan Late residing at Door No. 12\n" +
                "annual income is RS. 85000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "priya_income.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        // CRITICAL: Document holder must be Priya, NEVER replaced by Lathika
        assertEquals("Priya", response.getFields().get("holderName").getValue());
        IdentityVerificationResult idResult = response.getIdentityVerification();
        assertEquals("MISMATCH", idResult.getStatus());
        assertFalse(idResult.isNameMatch());
        assertFalse(idResult.isOverallMatch());
        assertTrue(idResult.getFailureReason().contains("does not match") || idResult.getFailureReason().contains("do not match"));
    }

    @Test
    @DisplayName("TEST 4: Income Certificate containing only OCR garbage returns null/NOT_FOUND")
    void testIncomeCertificate_GarbageOcr_UncertainOrMismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "xcvbnm qwertyuiop\n" +
                "### !!! %%% @@@\n" +
                "Income Certificate\n" +
                "RS. 50000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "garbage_income.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertNull(response.getFields().get("holderName").getValue(), "Holder name must be null for OCR garbage");
        assertEquals("NOT_FOUND", response.getFields().get("holderName").getStatus());
        assertTrue("UNCERTAIN".equals(response.getIdentityVerification().getStatus()) || "MISMATCH".equals(response.getIdentityVerification().getStatus()));
        assertFalse(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("TEST 5: Income Certificate where 'Lathika' appears only as location/authority MUST NOT MATCH")
    void testIncomeCertificate_LocationAuthorityOnly_DoNotMatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "GOVERNMENT OF TAMIL NADU\n" +
                "Revenue Department\n" +
                "This certificate was issued by Lathika District Authority\n" +
                "Income Certificate\n" +
                "Annual Income: Rs. 60000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "location_only.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        // Location term must NOT be extracted as candidate holder name
        assertNotEquals("Lathika", response.getFields().get("holderName").getValue());
        assertNotEquals("Lathika District Authority", response.getFields().get("holderName").getValue());
        assertFalse(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("TEST 7: Income Certificate with Tamil and English text extracts English holder name")
    void testIncomeCertificate_TamilAndEnglish() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "தமிழ்நாடு அரசு / Government of Tamil Nadu\n" +
                "வருமானச் சான்றிதழ் / Income Certificate\n" +
                "சான்றிதழ் எண் / Certificate No: TN-4202403185459\n" +
                "This is to certify. that Selvi Lathika daughter of Thiru Kumar Late residing at Door No. 428\n" +
                "குடும்ப ஆண்டு வருமானம் / Total Family Annual Income: Rs. 72000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "tamil_english_income.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("INCOME_CERTIFICATE", response.getDocumentType());
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
        assertTrue(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("1. testCommunityCertificate_LathikaWithOCRSpace")
    void testCommunityCertificate_LathikaWithOCRSpace() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Community Certificate\n" +
                "Certificate No: TN-COMM-2024-001\n" +
                "This is to certify that Selvi L athika daughter of Thiru Kumar Late residing at door No. 428, Kozhai...\n";

        MockMultipartFile file = new MockMultipartFile("file", "community_cert.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("COMMUNITY_CERTIFICATE", response.getDocumentType());
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertEquals("FOUND", response.getFields().get("holderName").getStatus());
        assertNull(response.getIdentityVerification().getDobMatch());
        assertTrue(response.getIdentityVerification().isNameMatch());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("2. testCommunityCertificate_LathikaNormal")
    void testCommunityCertificate_LathikaNormal() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Community Certificate\n" +
                "Certificate No: TN-COMM-2024-002\n" +
                "This is to certify that Selvi Lathika daughter of Thiru Kumar Late residing at door No. 428...\n";

        MockMultipartFile file = new MockMultipartFile("file", "community_cert.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
        assertTrue(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("3. testCommunityCertificate_LathikaSplitAtDifferentPositions")
    void testCommunityCertificate_LathikaSplitAtDifferentPositions() {
        assertEquals("Lathika", extractionService.normalizePersonNameCandidate("L athika"));
        assertEquals("Lathika", extractionService.normalizePersonNameCandidate("La thika"));
        assertEquals("Lathika", extractionService.normalizePersonNameCandidate("Lat hika"));
        assertEquals("Lathika", extractionService.normalizePersonNameCandidate("Lath ika"));
        assertEquals("Lathika", extractionService.normalizePersonNameCandidate("Lathik a"));

        // CRITICAL: Preserve legitimate multi-word names without improper merging
        assertEquals("Lathika Kumar", extractionService.normalizePersonNameCandidate("Lathika Kumar"));
        assertNotEquals("LathikaKumar", extractionService.normalizePersonNameCandidate("Lathika Kumar"));
        assertEquals("Lathika K", extractionService.normalizePersonNameCandidate("Lathika K"));
        assertEquals("K. Lathika", extractionService.normalizePersonNameCandidate("K. Lathika"));
        assertEquals("Selvi Lathika", extractionService.normalizePersonNameCandidate("Selvi Lathika"));
    }

    @Test
    @DisplayName("4. testCommunityCertificate_PriyaMismatch")
    void testCommunityCertificate_PriyaMismatch() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Community Certificate\n" +
                "Certificate No: TN-COMM-2024-003\n" +
                "This is to certify that Selvi Priya daughter of Thiru Kumar Late residing at door No. 428...\n";

        MockMultipartFile file = new MockMultipartFile("file", "community_cert_priya.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Priya", response.getFields().get("holderName").getValue());
        assertFalse(response.getIdentityVerification().isNameMatch());
        assertFalse(response.getIdentityVerification().isOverallMatch());
        assertEquals("MISMATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("5. testCommunityCertificate_DoesNotExtractFatherAsHolder")
    void testCommunityCertificate_DoesNotExtractFatherAsHolder() {
        String content = "This is to certify that Selvi Lathika daughter of Thiru Kumar Late residing at Kozhai";
        String holder = extractionService.extractCertificateHolderName(content, "COMMUNITY_CERTIFICATE");

        assertNotNull(holder);
        assertEquals("Lathika", holder);
        assertNotEquals("Kumar", holder);
        assertNotEquals("Lathika Kumar", holder);
    }

    @Test
    @DisplayName("6. testCommunityCertificate_DoesNotExtractLocationAsHolder")
    void testCommunityCertificate_DoesNotExtractLocationAsHolder() {
        String content = "Government of Tamil Nadu\n" +
                "Revenue Department, Cuddalore District\n" +
                "This is to certify that Selvi Lathika daughter of Thiru Kumar residing at Kozhai\n";
        String holder = extractionService.extractCertificateHolderName(content, "COMMUNITY_CERTIFICATE");

        assertNotNull(holder);
        assertEquals("Lathika", holder);
        assertNotEquals("Cuddalore", holder);
        assertNotEquals("Tamil Nadu", holder);
        assertNotEquals("Revenue", holder);
    }

    @Test
    @DisplayName("7. testIncomeCertificate_LathikaOCRFragment")
    void testIncomeCertificate_LathikaOCRFragment() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Income Certificate\n" +
                "Certificate No: TN-INC-2024-001\n" +
                "This is to certify that L athika daughter of Thiru Kumar residing at Kozhai\n" +
                "annual income is Rs. 75000/annum\n";

        MockMultipartFile file = new MockMultipartFile("file", "income_lathika_ocr.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "INCOME_CERTIFICATE", "Financial Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("8. testDomicileCertificate_LathikaOCRFragment")
    void testDomicileCertificate_LathikaOCRFragment() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "Domicile Certificate\n" +
                "Certificate No: TN-DOM-2024-001\n" +
                "This is to certify that L athika is a resident of Kozhai\n";

        MockMultipartFile file = new MockMultipartFile("file", "domicile_lathika_ocr.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "DOMICILE_CERTIFICATE", "Domicile Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }

    @Test
    @DisplayName("9. testNoProfileLeakageDuringOCRReconstruction")
    void testNoProfileLeakageDuringOCRReconstruction() {
        // When running performExtraction without any profile, reconstruction must work purely from document text
        String content = "This is to certify that L athika daughter of Thiru Kumar";
        StructuredDocumentExtractionResponse response = extractionService.performExtraction(
                content.getBytes(StandardCharsets.UTF_8),
                "test.txt",
                "text/plain",
                "COMMUNITY_CERTIFICATE",
                "Category Proof"
        );

        assertNotNull(response);
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        // Verify no profile interaction took place
        verifyNoInteractions(citizenProfileRepository);
    }

    @Test
    @DisplayName("10. testGenuineDifferentApplicant")
    void testGenuineDifferentApplicant() {
        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        String content = "This is to certify that Selvi Priya daughter of Thiru Murugan Late residing at Kozhai";
        MockMultipartFile file = new MockMultipartFile("file", "priya.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("Priya", response.getFields().get("holderName").getValue());
        assertEquals("MISMATCH", response.getIdentityVerification().getStatus());
        assertFalse(response.getIdentityVerification().isNameMatch());
        assertFalse(response.getIdentityVerification().isOverallMatch());
    }

    @Test
    @DisplayName("11. testRealCommunityCertificate_LathikaOriginals5")
    void testRealCommunityCertificate_LathikaOriginals5() throws Exception {
        File pdfFile = new File("e:/SCHEMEBRIDGE/test-documents/Lathika Originals-5.pdf");
        if (!pdfFile.exists()) {
            return;
        }

        CitizenProfile applicantProfile = CitizenProfile.builder()
                .userId("user-lathika")
                .displayName("Lathika.K")
                .dob(LocalDate.of(2007, 3, 22))
                .build();
        when(citizenProfileRepository.findByUserId("user-lathika")).thenReturn(Optional.of(applicantProfile));

        byte[] bytes = java.nio.file.Files.readAllBytes(pdfFile.toPath());
        MockMultipartFile file = new MockMultipartFile("file", "Lathika Originals-5.pdf", "application/pdf", bytes);
        StructuredDocumentExtractionResponse response = extractionService.extractAndVerify(file, "COMMUNITY_CERTIFICATE", "Category Proof", "user-lathika");

        assertNotNull(response);
        assertEquals("COMMUNITY_CERTIFICATE", response.getDocumentType());
        assertEquals("Lathika", response.getFields().get("holderName").getValue());
        assertEquals("FOUND", response.getFields().get("holderName").getStatus());
        assertNull(response.getIdentityVerification().getDobMatch());
        assertTrue(response.getIdentityVerification().isNameMatch());
        assertTrue(response.getIdentityVerification().isOverallMatch());
        assertEquals("MATCH", response.getIdentityVerification().getStatus());
    }
}


