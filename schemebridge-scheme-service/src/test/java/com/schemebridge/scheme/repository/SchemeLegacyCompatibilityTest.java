package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.*;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeLegacyCompatibilityTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SchemeRepository schemeRepository;

    @BeforeEach
    public void cleanUp() {
        schemeRepository.findBySchemeCode("REGRESS-LEGACY-001").ifPresent(schemeRepository::delete);
        schemeRepository.findBySchemeCode("REGRESS-STRUCT-001").ifPresent(schemeRepository::delete);
    }

    @Test
    public void testLegacyEligibilityConversion() {
        // Arrange: Insert a raw BSON document mimicking the legacy structure
        Document legacyDoc = new Document();
        legacyDoc.append("schemeCode", "REGRESS-LEGACY-001");
        legacyDoc.append("slug", "regress-legacy-slug");
        legacyDoc.append("status", "DRAFT");
        legacyDoc.append("version", 1);
        
        Document titleDoc = new Document().append("english", "Legacy Test Title");
        legacyDoc.append("title", titleDoc);

        Document descDoc = new Document().append("english", "Legacy Test Desc");
        legacyDoc.append("description", descDoc);

        List<String> legacyRules = List.of("Minimum 60% in class 12", "Family income below ₹6 LPA");
        legacyDoc.append("eligibilityRules", legacyRules);

        mongoTemplate.insert(legacyDoc, "schemes");

        // Act: Retrieve via Repository
        Optional<Scheme> retrievedOpt = schemeRepository.findBySchemeCode("REGRESS-LEGACY-001");

        // Assert
        assertTrue(retrievedOpt.isPresent());
        Scheme retrieved = retrievedOpt.get();
        assertNotNull(retrieved.getEligibilityRules());
        
        RuleGroup ruleGroup = retrieved.getEligibilityRules();
        assertEquals("ALL", ruleGroup.getLogicalOperator());
        assertEquals(2, ruleGroup.getConditions().size());

        EligibilityCondition c1 = ruleGroup.getConditions().get(0);
        assertEquals("LEGACY_FREE_TEXT", c1.getField());
        assertEquals(RuleOperator.EQUALS, c1.getOperator());
        assertEquals("Minimum 60% in class 12", c1.getValue());
        assertEquals("STRING", c1.getDataType());
        assertTrue(c1.isRequired());

        EligibilityCondition c2 = ruleGroup.getConditions().get(1);
        assertEquals("LEGACY_FREE_TEXT", c2.getField());
        assertEquals(RuleOperator.EQUALS, c2.getOperator());
        assertEquals("Family income below ₹6 LPA", c2.getValue());
        assertEquals("STRING", c2.getDataType());
        assertTrue(c2.isRequired());
    }

    @Test
    public void testStructuredEligibilityConversion() {
        // Arrange: Insert a structured RuleGroup via the standard entity
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

        Scheme scheme = Scheme.builder()
                .schemeCode("REGRESS-STRUCT-001")
                .slug("regress-struct-slug")
                .title(MultilingualText.builder().english("Struct Title").build())
                .description(MultilingualText.builder().english("Struct Desc").build())
                .eligibilityRules(eligibilityRules)
                .status(SchemeStatus.DRAFT)
                .version(1)
                .build();

        schemeRepository.save(scheme);

        // Act: Retrieve via Repository
        Optional<Scheme> retrievedOpt = schemeRepository.findBySchemeCode("REGRESS-STRUCT-001");

        // Assert
        assertTrue(retrievedOpt.isPresent());
        Scheme retrieved = retrievedOpt.get();
        assertNotNull(retrieved.getEligibilityRules());
        
        RuleGroup ruleGroup = retrieved.getEligibilityRules();
        assertEquals("ALL", ruleGroup.getLogicalOperator());
        assertEquals(1, ruleGroup.getConditions().size());

        EligibilityCondition c1 = ruleGroup.getConditions().get(0);
        assertEquals("AGE", c1.getField());
        assertEquals(RuleOperator.GREATER_THAN_OR_EQUAL, c1.getOperator());
        assertEquals("18", c1.getValue());
        assertEquals("NUMBER", c1.getDataType());
        assertTrue(c1.isRequired());
    }
}
