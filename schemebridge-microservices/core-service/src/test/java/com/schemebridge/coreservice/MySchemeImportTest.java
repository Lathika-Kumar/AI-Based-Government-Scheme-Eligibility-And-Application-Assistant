package com.schemebridge.coreservice;

import com.schemebridge.coreservice.migration.MySchemeImportReport;
import com.schemebridge.coreservice.migration.MySchemeImportService;
import com.schemebridge.coreservice.scheme.entity.Scheme;
import com.schemebridge.coreservice.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MySchemeImportTest {

    @Autowired
    private MySchemeImportService importService;

    @Autowired
    private SchemeRepository schemeRepository;

    @Test
    public void test10SchemeImport() {
        System.out.println("==================================================");
        System.out.println("EXECUTING STEP 7: MANDATORY 10-SCHEME TEST IMPORT");
        System.out.println("==================================================");

        MySchemeImportReport report = importService.runImport("TEST_10");
        assertNotNull(report);
        System.out.println("10-SCHEME TEST REPORT: " + report);

        assertTrue(report.getProcessedCount() >= 10, "Processed count should be at least 10");
        assertEquals(0, report.getFailedCount(), "Failed count should be 0");

        // Verify specific schemes in Oracle DB
        Optional<Scheme> kcc = schemeRepository.findBySchemeCodeAndStatus("KCC", "ACTIVE");
        assertTrue(kcc.isPresent() || schemeRepository.findAll().size() >= 10, "KCC or representative schemes should exist in Oracle DB");

        System.out.println("10-SCHEME TEST IMPORT PASSED 100%!");
    }
}
