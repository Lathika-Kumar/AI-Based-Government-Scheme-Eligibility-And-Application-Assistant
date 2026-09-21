package com.schemebridge.scheme.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.SchemeCreateRequest;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemeSeeder implements CommandLineRunner {

    private final SchemeRepository schemeRepository;
    private final SchemeCategoryRepository categoryRepository;
    private final Validator validator;

    @org.springframework.beans.factory.annotation.Value("${app.seeder.path:data/schemes/schemes.seed.json}")
    private String seedFilePath;

    @Override
    public void run(String... args) throws Exception {
        byte[] jsonData = null;
        Path seedPath = Paths.get(seedFilePath);
        if (Files.exists(seedPath)) {
            log.info("Starting database seeding from filesystem: {}...", seedFilePath);
            jsonData = Files.readAllBytes(seedPath);
        } else {
            org.springframework.core.io.ClassPathResource resource =
                    new org.springframework.core.io.ClassPathResource("data/schemes.seed.json");
            if (resource.exists()) {
                log.info("Starting database seeding from classpath resource: data/schemes.seed.json...");
                try (java.io.InputStream is = resource.getInputStream()) {
                    jsonData = is.readAllBytes();
                }
            } else {
                log.info("No seed file found at {} or classpath. Skipping auto-seeding.", seedFilePath);
                return;
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        List<SchemeCreateRequest> requests;
        try {
            requests = mapper.readValue(jsonData, new TypeReference<List<SchemeCreateRequest>>() {});
        } catch (Exception e) {
            log.error("Failed to parse seed file schemes.seed.json: {}", e.getMessage());
            return;
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int rejected = 0;

        Set<String> seenCodes = new HashSet<>();
        Set<String> seenSlugs = new HashSet<>();

        for (SchemeCreateRequest req : requests) {
            // 1. Validate bean constraints
            Set<ConstraintViolation<SchemeCreateRequest>> violations = validator.validate(req);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(java.util.stream.Collectors.joining("; "));
                log.warn("Seed record rejected (validation failed): code={}, reason={}", req.getSchemeCode(), details);
                rejected++;
                continue;
            }

            // 2. Validate category exists
            Optional<SchemeCategory> categoryOpt = categoryRepository.findByCode(req.getCategoryCode());
            if (categoryOpt.isEmpty()) {
                log.warn("Seed record rejected (category not found): code={}, category={}", req.getSchemeCode(), req.getCategoryCode());
                rejected++;
                continue;
            }

            // 3. Check duplicates in file
            if (seenCodes.contains(req.getSchemeCode())) {
                log.warn("Seed record rejected (duplicate schemeCode in file): code={}", req.getSchemeCode());
                rejected++;
                continue;
            }
            if (seenSlugs.contains(req.getSlug())) {
                log.warn("Seed record rejected (duplicate slug in file): slug={}", req.getSlug());
                rejected++;
                continue;
            }
            seenCodes.add(req.getSchemeCode());
            seenSlugs.add(req.getSlug());

            // 4. Validate schemeLevel enum
            SchemeLevel level = null;
            if (req.getSchemeLevel() != null && !req.getSchemeLevel().trim().isEmpty()) {
                try {
                    level = SchemeLevel.valueOf(req.getSchemeLevel().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Seed record rejected (invalid schemeLevel): code={}, level={}", req.getSchemeCode(), req.getSchemeLevel());
                    rejected++;
                    continue;
                }
            }

            // 5. Validate status enum
            SchemeStatus status = SchemeStatus.DRAFT;
            if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
                try {
                    status = SchemeStatus.valueOf(req.getStatus().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Seed record rejected (invalid status): code={}, status={}", req.getSchemeCode(), req.getStatus());
                    rejected++;
                    continue;
                }
            }

            int seedVersion = req.getVersion() != null ? req.getVersion() : 1;

            Optional<Scheme> existingOpt = schemeRepository.findBySchemeCode(req.getSchemeCode());

            if (existingOpt.isEmpty()) {
                if (schemeRepository.findBySlug(req.getSlug()).isPresent()) {
                    log.warn("Seed record rejected (slug exists in DB for another schemeCode): slug={}", req.getSlug());
                    rejected++;
                    continue;
                }

                Scheme scheme = createSchemeFromRequest(req, categoryOpt.get(), level, status, seedVersion);
                schemeRepository.save(scheme);
                inserted++;
                log.info("Seed record INSERTED: code={}", req.getSchemeCode());
            } else {
                Scheme existing = existingOpt.get();

                if (!existing.getSlug().equals(req.getSlug()) && schemeRepository.findBySlug(req.getSlug()).isPresent()) {
                    log.warn("Seed record rejected (updated slug exists in DB): slug={}", req.getSlug());
                    rejected++;
                    continue;
                }

                if (seedVersion > existing.getVersion()) {
                    updateSchemeFromRequest(existing, req, categoryOpt.get(), level, status, seedVersion);
                    schemeRepository.save(existing);
                    updated++;
                    log.info("Seed record UPDATED: code={}", req.getSchemeCode());
                } else {
                    skipped++;
                    log.info("Seed record SKIPPED (version {} <= DB version {}): code={}", seedVersion, existing.getVersion(), req.getSchemeCode());
                }
            }
        }

        log.info("Database Seeding Finished. Summary: Inserted={}, Updated={}, Skipped={}, Rejected={}", 
                inserted, updated, skipped, rejected);
    }

    private Scheme createSchemeFromRequest(SchemeCreateRequest req, SchemeCategory category, SchemeLevel level, SchemeStatus status, int version) {
        Instant lastVerifiedAt = req.getSource() != null ? req.getSource().getLastVerified() : null;
        return Scheme.builder()
                .schemeCode(req.getSchemeCode())
                .slug(req.getSlug())
                .title(req.getTitle())
                .description(req.getDescription())
                .shortDescription(req.getShortDescription())
                .category(SchemeCategoryRef.builder()
                        .code(category.getCode())
                        .name(category.getName())
                        .build())
                .department(req.getDepartment())
                .ministry(req.getMinistry())
                .schemeLevel(level)
                .stateOrUt(req.getStateOrUt())
                .beneficiaryType(req.getBeneficiaryType())
                .schemeType(req.getSchemeType())
                .eligibilityRules(req.getEligibilityRules())
                .benefits(req.getBenefits() != null ? req.getBenefits() : new ArrayList<>())
                .requiredDocuments(req.getRequiredDocuments() != null ? req.getRequiredDocuments() : new ArrayList<>())
                .applicationInfo(req.getApplicationInfo())
                .source(req.getSource())
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .faqs(req.getFaqs() != null ? req.getFaqs() : new ArrayList<>())
                .status(status)
                .version(version)
                .lastVerifiedAt(lastVerifiedAt)
                .build();
    }

    private void updateSchemeFromRequest(Scheme existing, SchemeCreateRequest req, SchemeCategory category, SchemeLevel level, SchemeStatus status, int version) {
        existing.setSlug(req.getSlug());
        existing.setTitle(req.getTitle());
        existing.setDescription(req.getDescription());
        existing.setShortDescription(req.getShortDescription());
        existing.setCategory(SchemeCategoryRef.builder()
                .code(category.getCode())
                .name(category.getName())
                .build());
        existing.setDepartment(req.getDepartment());
        existing.setMinistry(req.getMinistry());
        existing.setSchemeLevel(level);
        existing.setStateOrUt(req.getStateOrUt());
        existing.setBeneficiaryType(req.getBeneficiaryType());
        existing.setSchemeType(req.getSchemeType());
        existing.setEligibilityRules(req.getEligibilityRules());
        existing.setBenefits(req.getBenefits() != null ? req.getBenefits() : new ArrayList<>());
        existing.setRequiredDocuments(req.getRequiredDocuments() != null ? req.getRequiredDocuments() : new ArrayList<>());
        existing.setApplicationInfo(req.getApplicationInfo());
        existing.setSource(req.getSource());
        if (req.getSource() != null) {
            existing.setLastVerifiedAt(req.getSource().getLastVerified());
        }
        existing.setTags(req.getTags() != null ? req.getTags() : new ArrayList<>());
        existing.setFaqs(req.getFaqs() != null ? req.getFaqs() : new ArrayList<>());
        existing.setStatus(status);
        existing.setVersion(version);
    }
}


