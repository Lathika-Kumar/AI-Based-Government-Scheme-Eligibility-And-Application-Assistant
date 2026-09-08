package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.SchemeCategory;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CategorySeeder implements CommandLineRunner {

    private final SchemeCategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        Map<String, String> defaultCategories = Map.of(
            "EDU", "Education & Learning",
            "AGRI", "Agriculture & Rural Development",
            "HLTH", "Health & Wellness",
            "FIN", "Banking, Financial Services and Insurance",
            "SOC", "Social Welfare & Empowerment",
            "HOUS", "Housing & Shelter",
            "SKILL", "Skills & Employment",
            "WELF", "Women and Child Development"
        );

        for (Map.Entry<String, String> entry : defaultCategories.entrySet()) {
            if (categoryRepository.findByCode(entry.getKey()).isEmpty()) {
                SchemeCategory category = SchemeCategory.builder()
                        .code(entry.getKey())
                        .name(entry.getValue())
                        .description(entry.getValue())
                        .status("ACTIVE")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                categoryRepository.save(category);
                log.info("Auto-seeded default scheme category: code={}, name={}", entry.getKey(), entry.getValue());
            }
        }
    }
}
