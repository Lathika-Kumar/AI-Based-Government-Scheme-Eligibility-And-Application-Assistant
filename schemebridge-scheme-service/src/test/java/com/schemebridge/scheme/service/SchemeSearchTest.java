package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.response.PagedSchemeResponse;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SchemeSearchTest {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeSearchService schemeSearchService;

    @BeforeEach
    public void setUp() {
        // Clear all test documents
        schemeRepository.deleteAll();

        // Seed structured schemes for testing
        Scheme s1 = Scheme.builder()
                .schemeCode("SCH-SEARCH-01")
                .slug("national-scholarship-scheme")
                .title(MultilingualText.builder().english("National Scholarship Scheme").tamil("தேசிய கல்வி உதவித்தொகை").build())
                .description(MultilingualText.builder().english("Scholarship for students").tamil("மாணவர்களுக்கான உதவித்தொகை").build())
                .shortDescription(MultilingualText.builder().english("Short desc 1").tamil("சுருக்கம் 1").build())
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .ministry("Ministry of Education")
                .schemeLevel(SchemeLevel.CENTRAL)
                .beneficiaryType("STUDENTS")
                .schemeType("SCHOLARSHIP")
                .tags(List.of("education", "scholarship", "central"))
                .status(SchemeStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        Scheme s2 = Scheme.builder()
                .schemeCode("SCH-SEARCH-02")
                .slug("state-health-benefit")
                .title(MultilingualText.builder().english("State Health Benefit").tamil("மாநில சுகாதார உதவி").build())
                .description(MultilingualText.builder().english("Health insurance for farmers").tamil("விவசாயிகளுக்கான காப்பீடு").build())
                .shortDescription(MultilingualText.builder().english("Short desc 2").tamil("சுருக்கம் 2").build())
                .category(SchemeCategoryRef.builder().code("HLTH").name("Health").build())
                .ministry("Ministry of Health")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("TAMIL_NADU")
                .beneficiaryType("FARMERS")
                .schemeType("INSURANCE")
                .tags(List.of("health", "farmers", "state"))
                .status(SchemeStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(1800))
                .build();

        Scheme s3 = Scheme.builder()
                .schemeCode("SCH-SEARCH-03")
                .slug("draft-agriculture-support")
                .title(MultilingualText.builder().english("Draft Agriculture Support").tamil("வரைவு விவசாய ஆதரவு").build())
                .description(MultilingualText.builder().english("Under evaluation support").tamil("மதிப்பீட்டு ஆதரவு").build())
                .shortDescription(MultilingualText.builder().english("Short desc 3").tamil("சுருக்கம் 3").build())
                .category(SchemeCategoryRef.builder().code("AGRI").name("Agriculture").build())
                .ministry("Ministry of Agriculture")
                .schemeLevel(SchemeLevel.STATE)
                .stateOrUt("PUNJAB")
                .beneficiaryType("FARMERS")
                .schemeType("SUBSIDY")
                .tags(List.of("agriculture", "subsidy", "draft"))
                .status(SchemeStatus.DRAFT)
                .createdAt(Instant.now())
                .build();

        Scheme s4 = Scheme.builder()
                .schemeCode("SCH-SEARCH-04")
                .slug("inactive-sports-award")
                .title(MultilingualText.builder().english("Inactive Sports Award").tamil("செயலிழந்த விளையாட்டு விருது").build())
                .description(MultilingualText.builder().english("Award for athletes").tamil("விளையாட்டு வீரர்களுக்கான விருது").build())
                .shortDescription(MultilingualText.builder().english("Short desc 4").tamil("சுருக்கம் 4").build())
                .category(SchemeCategoryRef.builder().code("SPRT").name("Sports").build())
                .ministry("Ministry of Sports")
                .schemeLevel(SchemeLevel.CENTRAL)
                .beneficiaryType("ATHLETES")
                .schemeType("AWARD")
                .tags(List.of("sports", "award"))
                .status(SchemeStatus.INACTIVE)
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        schemeRepository.saveAll(List.of(s1, s2, s3, s4));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchByTitle_English() {
        PagedSchemeResponse res = schemeSearchService.search("Scholarship", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-01", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchByTitle_Tamil() {
        PagedSchemeResponse res = schemeSearchService.search("சுகாதார", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchByDescription() {
        PagedSchemeResponse res = schemeSearchService.search("farmers", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchByTag() {
        PagedSchemeResponse res = schemeSearchService.search("central", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-01", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSearchByMinistry() {
        PagedSchemeResponse res = schemeSearchService.search("Ministry of Health", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testFilterByCategory() {
        PagedSchemeResponse res = schemeSearchService.search(null, "EDU", null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-01", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testFilterBySchemeLevel() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, "STATE", null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements()); // Draft is also STATE but hidden for USER
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testFilterByState() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, "TAMIL_NADU", null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testFilterByBeneficiaryType() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, "STUDENTS", null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-01", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testFilterBySchemeType() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, "INSURANCE", null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testCombineFilters() {
        PagedSchemeResponse res = schemeSearchService.search("farmers", "HLTH", "STATE", "TAMIL_NADU", "FARMERS", null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testPagination() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 1, "schemeCode", "asc");
        assertEquals(2, res.getTotalElements()); // Active schemes count
        assertEquals(2, res.getTotalPages());
        assertEquals(1, res.getContent().size());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSortingAscending() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 10, "createdAt", "asc");
        assertEquals("SCH-SEARCH-02", res.getContent().get(1).getSchemeCode());
        assertEquals("SCH-SEARCH-01", res.getContent().get(0).getSchemeCode()); // s1 created 3600s ago, s2 1800s ago
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testSortingDescending() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 10, "createdAt", "desc");
        assertEquals("SCH-SEARCH-02", res.getContent().get(0).getSchemeCode());
        assertEquals("SCH-SEARCH-01", res.getContent().get(1).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testNoResultSearch() {
        PagedSchemeResponse res = schemeSearchService.search("nonexistentkeyword", null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(0, res.getTotalElements());
        assertTrue(res.getContent().isEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidPage() {
        assertThrows(IllegalArgumentException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, null, null, -1, 10, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidSizeNegative() {
        assertThrows(IllegalArgumentException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 0, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidSizeTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 51, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidSortField() {
        assertThrows(IllegalArgumentException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 10, "description", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testInvalidEnumFilter() {
        assertThrows(IllegalArgumentException.class, () -> 
            schemeSearchService.search(null, null, "INVALID_LEVEL", null, null, null, null, null, 0, 10, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserSeesActiveOnly() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(2, res.getTotalElements()); // Only s1 and s2 (ACTIVE)
        res.getContent().forEach(s -> assertEquals(SchemeStatus.ACTIVE, s.getStatus()));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserCannotAccessDraft() {
        assertThrows(SecurityException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, "DRAFT", null, 0, 10, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserCannotAccessInactive() {
        assertThrows(SecurityException.class, () -> 
            schemeSearchService.search(null, null, null, null, null, null, "INACTIVE", null, 0, 10, "schemeCode", "asc")
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminCanAccessDraft() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, "DRAFT", null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-03", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminCanAccessInactive() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, "INACTIVE", null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-04", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "SCHEME_MANAGER")
    public void testSchemeManagerCanAccessDraft() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, "DRAFT", null, 0, 10, "schemeCode", "asc");
        assertEquals(1, res.getTotalElements());
        assertEquals("SCH-SEARCH-03", res.getContent().get(0).getSchemeCode());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testMultiValueFilter_SchemeLevel() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, "CENTRAL,STATE", null, null, null, null, null, 0, 10, "schemeCode", "asc");
        assertEquals(2, res.getTotalElements()); // s1 (CENTRAL) and s2 (STATE)
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testMultiValueFilter_Tags() {
        PagedSchemeResponse res = schemeSearchService.search(null, null, null, null, null, null, null, "central,state", 0, 10, "schemeCode", "asc");
        assertEquals(2, res.getTotalElements()); // s1 has "central", s2 has "state"
    }
}
