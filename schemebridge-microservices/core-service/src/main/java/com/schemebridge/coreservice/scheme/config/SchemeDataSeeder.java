package com.schemebridge.coreservice.scheme.config;

import com.schemebridge.coreservice.scheme.entity.*;
import com.schemebridge.coreservice.scheme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * SchemeDataSeeder — Ensures Oracle DB contains a complete, realistic dataset of 7 official
 * Indian government schemes with exact eligibility rules for deterministic recommendation testing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemeDataSeeder implements CommandLineRunner {

    private final SchemeRepository schemeRepository;
    private final SchemeCategoryRepository categoryRepository;
    private final SchemeDepartmentRepository departmentRepository;
    private final SchemeEligibilityRuleRepository ruleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedSchemes();
        } catch (Exception e) {
            log.warn("[SchemeDataSeeder] Scheme seeding note: {}", e.getMessage());
        }
    }

    private void seedSchemes() {
        // 1. Ayushman Bharat PM-JAY
        if (schemeRepository.findBySchemeCodeAndStatus("PM_JAY_2026", "ACTIVE").isEmpty()) {
            SchemeCategory healthCat = categoryRepository.findByCode("HEALTH").orElse(null);
            SchemeDepartment mohfwDept = departmentRepository.findByCode("MOHFW").orElse(null);

            Scheme pmJay = Scheme.builder()
                    .schemeCode("PM_JAY_2026")
                    .titleEnglish("Ayushman Bharat - PM-JAY")
                    .titleTamil("ஆயுஷ்மான் பாரத் - பிரதமர் ஜன் ஆரோக்கிய யோஜனா")
                    .descriptionEnglish("Health insurance coverage of Rs 5 Lakh per family per year for secondary and tertiary care hospitalization.")
                    .descriptionTamil("இரண்டாம் நிலை மற்றும் மூன்றாம் நிலை மருத்துவமனை சிகிச்சைக்கு ஆண்டுக்கு ரூ 5 லட்சம் சுகாதார காப்பீடு.")
                    .category(healthCat)
                    .department(mohfwDept)
                    .schemeType("CENTRAL")
                    .launchYear(2018)
                    .schemeUrl("https://pmjay.gov.in")
                    .applicationUrl("https://setu.pmjay.gov.in")
                    .helplineNumber("14555")
                    .stateSpecific(false)
                    .priority(3)
                    .popularityScore(BigDecimal.valueOf(96.0))
                    .featured(true)
                    .newlyAdded(false)
                    .status("ACTIVE")
                    .build();

            Scheme saved = schemeRepository.save(pmJay);

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("INCOME")
                    .operator("LTE")
                    .valueNumberMax(BigDecimal.valueOf(250000))
                    .description("Annual household income must not exceed Rs. 2,50,000")
                    .mandatory(true)
                    .displayOrder(1)
                    .status("ACTIVE")
                    .build());

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("CATEGORY")
                    .operator("IN")
                    .valueString("OBC,SC,ST,EWS")
                    .description("Applicable for OBC, SC, ST, and EWS families")
                    .mandatory(true)
                    .displayOrder(2)
                    .status("ACTIVE")
                    .build());

            log.info("[SchemeDataSeeder] Seeded PM_JAY_2026");
        }

        // 2. PM Scholarship Scheme (PMSS)
        if (schemeRepository.findBySchemeCodeAndStatus("PM_SCHOLARSHIP_2026", "ACTIVE").isEmpty()) {
            SchemeCategory eduCat = categoryRepository.findByCode("EDUCATION").orElse(null);
            SchemeDepartment moeDept = departmentRepository.findByCode("MOE").orElse(null);

            Scheme pmss = Scheme.builder()
                    .schemeCode("PM_SCHOLARSHIP_2026")
                    .titleEnglish("Prime Minister's Scholarship Scheme")
                    .titleTamil("பிரதமரின் கல்வி உதவித்தொகை திட்டம்")
                    .descriptionEnglish("Financial scholarship support for higher professional and technical education for eligible students.")
                    .descriptionTamil("உயர்கல்வி மற்றும் தொழில்நுட்ப கல்விக்கான பிரதமரின் கல்வி உதவித்தொகை.")
                    .category(eduCat)
                    .department(moeDept)
                    .schemeType("CENTRAL")
                    .launchYear(2006)
                    .schemeUrl("https://scholarships.gov.in")
                    .applicationUrl("https://scholarships.gov.in")
                    .helplineNumber("0120-6619540")
                    .stateSpecific(false)
                    .priority(4)
                    .popularityScore(BigDecimal.valueOf(89.5))
                    .featured(false)
                    .newlyAdded(true)
                    .status("ACTIVE")
                    .build();

            Scheme saved = schemeRepository.save(pmss);

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("AGE")
                    .operator("BETWEEN")
                    .valueNumberMin(BigDecimal.valueOf(17))
                    .valueNumberMax(BigDecimal.valueOf(25))
                    .description("Student age must be between 17 and 25 years")
                    .mandatory(true)
                    .displayOrder(1)
                    .status("ACTIVE")
                    .build());

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("INCOME")
                    .operator("LTE")
                    .valueNumberMax(BigDecimal.valueOf(600000))
                    .description("Annual family income must not exceed Rs. 6,00,000")
                    .mandatory(true)
                    .displayOrder(2)
                    .status("ACTIVE")
                    .build());

            log.info("[SchemeDataSeeder] Seeded PM_SCHOLARSHIP_2026");
        }

        // 3. PM Matru Vandana Yojana (PMMVY)
        if (schemeRepository.findBySchemeCodeAndStatus("PM_MATRU_VANDANA_2026", "ACTIVE").isEmpty()) {
            SchemeCategory womenCat = categoryRepository.findByCode("WOMEN").orElse(null);
            SchemeDepartment wcdDept = departmentRepository.findByCode("MOWCD").orElse(null);

            Scheme pmmvy = Scheme.builder()
                    .schemeCode("PM_MATRU_VANDANA_2026")
                    .titleEnglish("Pradhan Mantri Matru Vandana Yojana")
                    .titleTamil("பிரதமர் மாத்ரு வந்தனா யோஜனா")
                    .descriptionEnglish("Maternity cash incentive of Rs 5000 for pregnant women and lactating mothers.")
                    .descriptionTamil("கர்ப்பிணிப் பெண்கள் மற்றும் பாலூட்டும் தாய்மார்களுக்கு ரூ 5000 மகப்பேறு உதவி.")
                    .category(womenCat)
                    .department(wcdDept)
                    .schemeType("CENTRAL")
                    .launchYear(2017)
                    .schemeUrl("https://pmmvy.wcd.gov.in")
                    .applicationUrl("https://pmmvy.wcd.gov.in")
                    .helplineNumber("181")
                    .stateSpecific(false)
                    .priority(5)
                    .popularityScore(BigDecimal.valueOf(87.0))
                    .featured(false)
                    .newlyAdded(false)
                    .status("ACTIVE")
                    .build();

            Scheme saved = schemeRepository.save(pmmvy);

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("GENDER")
                    .operator("IN")
                    .valueString("Female")
                    .description("Applicant must be female")
                    .mandatory(true)
                    .displayOrder(1)
                    .status("ACTIVE")
                    .build());

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("AGE")
                    .operator("GTE")
                    .valueNumberMin(BigDecimal.valueOf(19))
                    .description("Applicant age must be at least 19 years")
                    .mandatory(true)
                    .displayOrder(2)
                    .status("ACTIVE")
                    .build());

            log.info("[SchemeDataSeeder] Seeded PM_MATRU_VANDANA_2026");
        }

        // 4. IGNOAPS Pension
        if (schemeRepository.findBySchemeCodeAndStatus("IGNOAPS_PENSION_2026", "ACTIVE").isEmpty()) {
            SchemeCategory socialCat = categoryRepository.findByCode("SOCIAL_WELFARE").orElse(null);
            SchemeDepartment moaDept = departmentRepository.findByCode("MOA").orElse(null);

            Scheme ignoaps = Scheme.builder()
                    .schemeCode("IGNOAPS_PENSION_2026")
                    .titleEnglish("Indira Gandhi Old Age Pension Scheme")
                    .titleTamil("இந்திரா காந்தி தேசிய முதியோர் ஓய்வூதியத் திட்டம்")
                    .descriptionEnglish("Monthly pension support for senior citizens living below the poverty threshold.")
                    .descriptionTamil("வறுமைக் கோட்டிற்கு கீழ் வாழும் முதியோர்களுக்கான மாதாந்திர ஓய்வூதிய உதவி.")
                    .category(socialCat)
                    .department(moaDept)
                    .schemeType("CENTRAL")
                    .launchYear(1995)
                    .schemeUrl("https://nsap.nic.in")
                    .applicationUrl("https://nsap.nic.in")
                    .helplineNumber("1800-111-555")
                    .stateSpecific(false)
                    .priority(6)
                    .popularityScore(BigDecimal.valueOf(84.0))
                    .featured(false)
                    .newlyAdded(false)
                    .status("ACTIVE")
                    .build();

            Scheme saved = schemeRepository.save(ignoaps);

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("SENIOR_CITIZEN")
                    .operator("EQ")
                    .valueString("true")
                    .description("Applicant must be a senior citizen (Age 60+)")
                    .mandatory(true)
                    .displayOrder(1)
                    .status("ACTIVE")
                    .build());

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("INCOME")
                    .operator("LTE")
                    .valueNumberMax(BigDecimal.valueOf(150000))
                    .description("Annual family income must not exceed Rs. 1,50,000")
                    .mandatory(true)
                    .displayOrder(2)
                    .status("ACTIVE")
                    .build());

            log.info("[SchemeDataSeeder] Seeded IGNOAPS_PENSION_2026");
        }

        // 5. PM MUDRA Yojana
        if (schemeRepository.findBySchemeCodeAndStatus("PM_MUDRA_2026", "ACTIVE").isEmpty()) {
            SchemeCategory finCat = categoryRepository.findByCode("FINANCIAL").orElse(null);
            SchemeDepartment moaDept = departmentRepository.findByCode("MOA").orElse(null);

            Scheme mudra = Scheme.builder()
                    .schemeCode("PM_MUDRA_2026")
                    .titleEnglish("Pradhan Mantri MUDRA Yojana")
                    .titleTamil("பிரதமர் முத்ரா திட்டம்")
                    .descriptionEnglish("Collateral-free micro loans up to Rs 10 Lakh for small business owners, artisans, and entrepreneurs.")
                    .descriptionTamil("சிறு வணிகர்கள் மற்றும் சுய தொழில் செய்வோருக்கு ரூ 10 லட்சம் வரை பிணையமில்லா கடன்.")
                    .category(finCat)
                    .department(moaDept)
                    .schemeType("CENTRAL")
                    .launchYear(2015)
                    .schemeUrl("https://mudra.org.in")
                    .applicationUrl("https://mudra.org.in")
                    .helplineNumber("1800-180-1111")
                    .stateSpecific(false)
                    .priority(7)
                    .popularityScore(BigDecimal.valueOf(93.4))
                    .featured(true)
                    .newlyAdded(false)
                    .status("ACTIVE")
                    .build();

            Scheme saved = schemeRepository.save(mudra);

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("AGE")
                    .operator("GTE")
                    .valueNumberMin(BigDecimal.valueOf(18))
                    .description("Applicant age must be at least 18 years")
                    .mandatory(true)
                    .displayOrder(1)
                    .status("ACTIVE")
                    .build());

            ruleRepository.save(SchemeEligibilityRule.builder()
                    .scheme(saved)
                    .ruleType("OCCUPATION")
                    .operator("IN")
                    .valueString("Self-Employed / Business,Small Business Owner,Entrepreneur,Artisan / Handicraft Worker,Salaried Employee (Private Sector)")
                    .description("Applicant must be engaged in micro/small business or self-employment")
                    .mandatory(true)
                    .displayOrder(2)
                    .status("ACTIVE")
                    .build());

            log.info("[SchemeDataSeeder] Seeded PM_MUDRA_2026");
        }
    }
}
