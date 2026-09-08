package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.CitizenEligibilityProfile;
import com.schemebridge.scheme.dto.request.CreateApplicationRequest;
import com.schemebridge.scheme.dto.response.*;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.ApplicationDocumentRepository;
import com.schemebridge.scheme.repository.ApplicationRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeApplicationTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDocumentRepository applicationDocumentRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private DocumentStorageService documentStorageService;

    @Autowired
    private MongoTemplate mongoTemplate;

    private Scheme activeScheme;
    private Scheme inactiveScheme;
    private static final String TEST_USER = "test-user-123";
    private static final String OTHER_USER = "other-user-456";

    @BeforeEach
    public void setUp() {
        // Clean up tests DB
        applicationDocumentRepository.deleteAll();
        applicationRepository.deleteAll();
        schemeRepository.findBySchemeCode("APP-TEST-ACTIVE").ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode("APP-TEST-INACTIVE").ifPresent(schemeRepository::delete);

        // Required Documents
        RequiredDocument reqDoc1 = RequiredDocument.builder()
                .documentCode("AADHAAR")
                .name(MultilingualText.builder().english("Aadhaar Card").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG"))
                .build();

        RequiredDocument reqDoc2 = RequiredDocument.builder()
                .documentCode("INCOME_CERT")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .mandatory(false)
                .acceptedFormats(List.of("PDF"))
                .build();

        // Eligibility Condition (Age >= 18)
        EligibilityCondition ageCondition = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .required(true)
                .build();

        RuleGroup rules = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(ageCondition))
                .build();

        activeScheme = Scheme.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .slug("app-test-active")
                .title(MultilingualText.builder().english("Active Test Scheme").build())
                .description(MultilingualText.builder().english("Active test desc").build())
                .status(SchemeStatus.ACTIVE)
                .eligibilityRules(rules)
                .requiredDocuments(List.of(reqDoc1, reqDoc2))
                .version(1)
                .build();

        inactiveScheme = Scheme.builder()
                .schemeCode("APP-TEST-INACTIVE")
                .slug("app-test-inactive")
                .title(MultilingualText.builder().english("Inactive Test Scheme").build())
                .description(MultilingualText.builder().english("Inactive test desc").build())
                .status(SchemeStatus.DRAFT)
                .eligibilityRules(rules)
                .requiredDocuments(List.of(reqDoc1))
                .version(1)
                .build();

        schemeRepository.save(activeScheme);
        schemeRepository.save(inactiveScheme);
    }

    @Test
    public void testCreateApplication_Eligible_Success() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(20)
                .build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();

        ApplicationResponse res = applicationService.createApplication(req, TEST_USER);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals("APP-TEST-ACTIVE", res.getSchemeCode());
        assertEquals(TEST_USER, res.getUserId());
        assertEquals(ApplicationStatus.DOCUMENTS_PENDING.name(), res.getStatus());
        assertNotNull(res.getApplicationNumber());
        assertTrue(res.getApplicationNumber().startsWith("SB-APP-2026-"));

        // Verify document tracking copies
        List<ApplicationDocument> docs = applicationDocumentRepository.findAllByApplicationId(res.getId());
        assertEquals(2, docs.size());
        
        ApplicationDocument aadhaar = docs.stream().filter(d -> d.getDocumentCode().equals("AADHAAR")).findFirst().orElse(null);
        assertNotNull(aadhaar);
        assertTrue(aadhaar.isMandatory());
        assertFalse(aadhaar.isUploaded());

        ApplicationDocument income = docs.stream().filter(d -> d.getDocumentCode().equals("INCOME_CERT")).findFirst().orElse(null);
        assertNotNull(income);
        assertFalse(income.isMandatory());
        assertFalse(income.isUploaded());
    }

    @Test
    public void testCreateApplication_Ineligible_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(16) // Under 18
                .build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.createApplication(req, TEST_USER);
        });
    }

    @Test
    public void testCreateApplication_Indeterminate_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                // Omit Age
                .build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.createApplication(req, TEST_USER);
        });
    }

    @Test
    public void testCreateApplication_InactiveScheme_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(22)
                .build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-INACTIVE")
                .profile(profile)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.createApplication(req, TEST_USER);
        });
    }

    @Test
    public void testCreateApplication_DuplicateActive_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder()
                .age(20)
                .build();

        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();

        applicationService.createApplication(req, TEST_USER);

        // Try creating again while the first one is active (DOCUMENTS_PENDING status)
        assertThrows(DuplicateResourceException.class, () -> {
            applicationService.createApplication(req, TEST_USER);
        });
    }

    @Test
    @WithMockUser(username = TEST_USER)
    public void testGetApplicationDetails_Owner_Success() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        ApplicationResponse fetched = applicationService.getApplicationDetails(created.getId(), TEST_USER);
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("Active Test Scheme", fetched.getSchemeTitle().getEnglish());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    public void testGetApplicationDetails_AnotherUser_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        assertThrows(SecurityException.class, () -> {
            applicationService.getApplicationDetails(created.getId(), OTHER_USER);
        });
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    public void testGetApplicationDetails_Admin_Success() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        // Admin can fetch details of any user's application
        ApplicationResponse fetched = applicationService.getApplicationDetails(created.getId(), "adminUser");
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    public void testUploadDocument_Success() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        MockMultipartFile mockFile = new MockMultipartFile("file", "aadhaar_test.pdf", "application/pdf", "dummy content".getBytes());
        ApplicationDocumentResponse docRes = applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, TEST_USER);

        assertNotNull(docRes);
        assertTrue(docRes.isUploaded());
        assertEquals("aadhaar_test.pdf", docRes.getFileName());
        assertEquals(DocumentVerificationStatus.PENDING.name(), docRes.getVerificationStatus());

        // Verify Application readiness updates to READY_FOR_SUBMISSION since Aadhaar is the only mandatory document
        ApplicationResponse appDetails = applicationService.getApplicationDetails(created.getId(), TEST_USER);
        assertEquals(ApplicationStatus.READY_FOR_SUBMISSION.name(), appDetails.getStatus());
        assertEquals(50, appDetails.getDocumentReadiness().getPercentage()); // 1 out of 2 uploaded
        assertEquals(0, appDetails.getDocumentReadiness().getMandatoryMissing());
    }

    @Test
    public void testUploadDocument_InvalidFormat_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        // AADHAAR only accepts PDF or JPG in activeScheme, upload PNG
        MockMultipartFile mockFile = new MockMultipartFile("file", "aadhaar_test.png", "image/png", "dummy content".getBytes());
        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, TEST_USER);
        });
    }

    @Test
    public void testUploadDocument_Oversized_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        // Create a 6MB dummy file
        byte[] oversizedBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile mockFile = new MockMultipartFile("file", "aadhaar_large.pdf", "application/pdf", oversizedBytes);
        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, TEST_USER);
        });
    }

    @Test
    public void testUploadDocument_AnotherUser_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        MockMultipartFile mockFile = new MockMultipartFile("file", "aadhaar_test.pdf", "application/pdf", "dummy content".getBytes());
        assertThrows(SecurityException.class, () -> {
            applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, OTHER_USER);
        });
    }

    @Test
    public void testSubmitApplication_MissingMandatory_ThrowsException() {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        // Try submitting without uploading Aadhaar (mandatory)
        assertThrows(IllegalArgumentException.class, () -> {
            applicationService.submitApplication(created.getId(), TEST_USER);
        });
    }

    @Test
    @WithMockUser(username = TEST_USER)
    public void testSubmitApplication_Success() throws Exception {
        CitizenEligibilityProfile profile = CitizenEligibilityProfile.builder().age(20).build();
        CreateApplicationRequest req = CreateApplicationRequest.builder()
                .schemeCode("APP-TEST-ACTIVE")
                .profile(profile)
                .build();
        ApplicationResponse created = applicationService.createApplication(req, TEST_USER);

        // Upload Aadhaar (mandatory)
        MockMultipartFile mockFile = new MockMultipartFile("file", "aadhaar_test.pdf", "application/pdf", "dummy content".getBytes());
        applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, TEST_USER);

        // Submit
        ApplicationResponse submitted = applicationService.submitApplication(created.getId(), TEST_USER);
        assertNotNull(submitted);
        assertEquals(ApplicationStatus.SUBMITTED.name(), submitted.getStatus());
        assertNotNull(submitted.getSubmittedAt());

        // Submitted application cannot be modified with document uploads
        assertThrows(IllegalStateException.class, () -> {
            applicationService.uploadDocument(created.getId(), "AADHAAR", mockFile, TEST_USER);
        });
    }

    @Test
    public void testStorageService_StoreAndRetrieve() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "store_and_retrieve_test".getBytes());
        String ref = documentStorageService.store("app-123", "DOC", mockFile);
        assertNotNull(ref);
        assertFalse(ref.isBlank());

        try (InputStream is = documentStorageService.retrieve(ref)) {
            byte[] bytes = is.readAllBytes();
            assertEquals("store_and_retrieve_test", new String(bytes));
        }

        documentStorageService.delete(ref);
    }


    @Test
    public void testMongoDBIndexesCreatedCorrectly() {
        // Assert applications index definition
        var appIndexes = mongoTemplate.getCollection("applications").listIndexes();
        boolean foundUniqueAppNum = false;
        boolean foundUserSchemeCompound = false;
        
        for (org.bson.Document idx : appIndexes) {
            String name = idx.getString("name");
            if (name.equals("applicationNumber_1") || name.equals("applicationNumber")) {
                foundUniqueAppNum = idx.getBoolean("unique", false);
            }
            if (name.equals("user_scheme_idx")) {
                foundUserSchemeCompound = true;
            }
        }
        
        assertTrue(foundUniqueAppNum, "Unique index on applicationNumber not found");
        assertTrue(foundUserSchemeCompound, "Compound index user_scheme_idx not found");

        // Assert application_documents index definition
        var docIndexes = mongoTemplate.getCollection("application_documents").listIndexes();
        boolean foundAppDocUniqueCompound = false;
        
        for (org.bson.Document idx : docIndexes) {
            String name = idx.getString("name");
            if (name.equals("app_doc_code_idx")) {
                foundAppDocUniqueCompound = idx.getBoolean("unique", false);
            }
        }
        
        assertTrue(foundAppDocUniqueCompound, "Compound unique index app_doc_code_idx not found");
    }
}
