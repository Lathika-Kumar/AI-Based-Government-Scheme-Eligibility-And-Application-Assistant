package com.schemebridge.scheme.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SchemeSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    public void testEligibilityConditionSerialization() throws Exception {
        EligibilityCondition condition = EligibilityCondition.builder()
                .field("AGE")
                .operator(RuleOperator.GREATER_THAN_OR_EQUAL)
                .value("18")
                .dataType("NUMBER")
                .question(MultilingualText.builder().english("Are you 18 or older?").tamil("உங்களுக்கு 18 வயதாகிவிட்டதா?").build())
                .required(true)
                .sourceReference("Rule 1.1")
                .build();

        String json = mapper.writeValueAsString(condition);
        EligibilityCondition deserialized = mapper.readValue(json, EligibilityCondition.class);

        assertEquals("AGE", deserialized.getField());
        assertEquals(RuleOperator.GREATER_THAN_OR_EQUAL, deserialized.getOperator());
        assertEquals("18", deserialized.getValue());
        assertTrue(deserialized.isRequired());
        assertEquals("Are you 18 or older?", deserialized.getQuestion().getEnglish());
        assertEquals("உங்களுக்கு 18 வயதாகிவிட்டதா?", deserialized.getQuestion().getTamil());
    }

    @Test
    public void testNestedRuleGroups() throws Exception {
        EligibilityCondition c1 = EligibilityCondition.builder().field("AGE").operator(RuleOperator.GREATER_THAN).value("18").build();
        EligibilityCondition c2 = EligibilityCondition.builder().field("INCOME").operator(RuleOperator.LESS_THAN).value("250000").build();
        
        RuleGroup innerGroup = RuleGroup.builder()
                .logicalOperator("ANY")
                .conditions(List.of(
                        EligibilityCondition.builder().field("CATEGORY").operator(RuleOperator.EQUALS).value("SC").build(),
                        EligibilityCondition.builder().field("CATEGORY").operator(RuleOperator.EQUALS).value("ST").build()
                ))
                .build();

        RuleGroup rootGroup = RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(List.of(c1, c2))
                .groups(List.of(innerGroup))
                .build();

        String json = mapper.writeValueAsString(rootGroup);
        RuleGroup deserialized = mapper.readValue(json, RuleGroup.class);

        assertEquals("ALL", deserialized.getLogicalOperator());
        assertEquals(2, deserialized.getConditions().size());
        assertEquals(1, deserialized.getGroups().size());
        assertEquals("ANY", deserialized.getGroups().get(0).getLogicalOperator());
        assertEquals(2, deserialized.getGroups().get(0).getConditions().size());
    }

    @Test
    public void testSchemeBenefitSerialization() throws Exception {
        SchemeBenefit benefit = SchemeBenefit.builder()
                .title(MultilingualText.builder().english("Scholarship").tamil("கல்வி உதவித்தொகை").build())
                .amount(new BigDecimal("50000.00"))
                .amountType("SCHOLARSHIP")
                .frequency("ANNUALLY")
                .duration("1 Year")
                .conditions(List.of("Pass exams"))
                .build();

        String json = mapper.writeValueAsString(benefit);
        SchemeBenefit deserialized = mapper.readValue(json, SchemeBenefit.class);

        assertEquals(new BigDecimal("50000.00"), deserialized.getAmount());
        assertEquals("SCHOLARSHIP", deserialized.getAmountType());
        assertEquals("ANNUALLY", deserialized.getFrequency());
        assertEquals("Pass exams", deserialized.getConditions().get(0));
    }

    @Test
    public void testRequiredDocumentSerialization() throws Exception {
        RequiredDocument document = RequiredDocument.builder()
                .documentCode("INCOME_CERTIFICATE")
                .name(MultilingualText.builder().english("Income Certificate").build())
                .mandatory(true)
                .acceptedFormats(List.of("PDF", "JPG"))
                .issuingAuthority("Tahsildar")
                .build();

        String json = mapper.writeValueAsString(document);
        RequiredDocument deserialized = mapper.readValue(json, RequiredDocument.class);

        assertEquals("INCOME_CERTIFICATE", deserialized.getDocumentCode());
        assertTrue(deserialized.isMandatory());
        assertTrue(deserialized.getAcceptedFormats().contains("PDF"));
    }

    @Test
    public void testApplicationInfoSerialization() throws Exception {
        Instant now = Instant.now();
        ApplicationInfo info = ApplicationInfo.builder()
                .applicationMode("ONLINE")
                .applicationUrl("https://scholarships.gov.in")
                .applicationStartDate(now)
                .deadline(now)
                .build();

        String json = mapper.writeValueAsString(info);
        ApplicationInfo deserialized = mapper.readValue(json, ApplicationInfo.class);

        assertEquals("ONLINE", deserialized.getApplicationMode());
        assertEquals("https://scholarships.gov.in", deserialized.getApplicationUrl());
        assertEquals(now, deserialized.getApplicationStartDate());
        assertEquals(now, deserialized.getDeadline());
    }

    @Test
    public void testSourceMetadataSerialization() throws Exception {
        SourceMetadata metadata = SourceMetadata.builder()
                .sourceType("OFFICIAL_CENTRAL_GOVERNMENT")
                .sourceUrl("https://india.gov.in")
                .verificationStatus("VERIFIED")
                .build();

        String json = mapper.writeValueAsString(metadata);
        SourceMetadata deserialized = mapper.readValue(json, SourceMetadata.class);

        assertEquals("OFFICIAL_CENTRAL_GOVERNMENT", deserialized.getSourceType());
        assertEquals("https://india.gov.in", deserialized.getSourceUrl());
        assertEquals("VERIFIED", deserialized.getVerificationStatus());
    }
}
