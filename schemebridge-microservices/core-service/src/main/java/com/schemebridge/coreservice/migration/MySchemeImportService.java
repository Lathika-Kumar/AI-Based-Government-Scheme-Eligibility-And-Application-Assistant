package com.schemebridge.coreservice.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.coreservice.scheme.entity.*;
import com.schemebridge.coreservice.scheme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MySchemeImportService {

    private final SchemeRepository schemeRepository;
    private final SchemeCategoryRepository categoryRepository;
    private final SchemeDepartmentRepository departmentRepository;
    private final MySchemeJsonParser jsonParser;
    private final MySchemeMapper mapper;

    private final Map<String, SchemeCategory> categoryCache = new ConcurrentHashMap<>();
    private final Map<String, SchemeDepartment> departmentCache = new ConcurrentHashMap<>();

    public MySchemeImportReport runImport(String mode) {
        long startTime = System.currentTimeMillis();
        log.info("==================================================");
        log.info("Starting Phase 2B myScheme Data Import [Mode: {}]...", mode);

        File detailsDir = new File("d:/schemeBridge/myscheme-data/raw/details");
        if (!detailsDir.exists() || !detailsDir.isDirectory()) {
            // Try relative fallback
            detailsDir = new File("../myscheme-data/raw/details");
        }

        File[] files = detailsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) files = new File[0];

        int totalFound = files.length;
        log.info("Found {} total detail JSON files in {}", totalFound, detailsDir.getAbsolutePath());

        List<File> targetFiles = new ArrayList<>();
        if ("TEST_10".equalsIgnoreCase(mode)) {
            // Select 10 specific representative schemes
            List<String> sampleSlugs = List.of("kcc", "pm-kisan", "pmay-u", "apy", "ab-pmjay", "mgnrega", "108easuk", "15dsugt", "1pmy", "40shydcs");
            for (String slug : sampleSlugs) {
                File sf = new File(detailsDir, slug + ".json");
                if (sf.exists()) targetFiles.add(sf);
            }
            // Fill remaining if any sample missing
            for (File f : files) {
                if (targetFiles.size() >= 10) break;
                if (!targetFiles.contains(f)) targetFiles.add(f);
            }
            log.info("Selected {} representative schemes for 10-SCHEME TEST IMPORT.", targetFiles.size());
        } else {
            targetFiles = Arrays.asList(files);
            log.info("Executing FULL IMPORT of all {} schemes.", targetFiles.size());
        }

        // Initialize category/department caches
        preloadCaches();

        int processed = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errorLogs = new ArrayList<>();

        int batchSize = 100;
        List<File> currentBatch = new ArrayList<>();

        for (int i = 0; i < targetFiles.size(); i++) {
            currentBatch.add(targetFiles.get(i));
            if (currentBatch.size() == batchSize || i == targetFiles.size() - 1) {
                BatchResult bRes = processBatch(currentBatch);
                processed += bRes.processed;
                inserted += bRes.inserted;
                updated += bRes.updated;
                skipped += bRes.skipped;
                failed += bRes.failed;
                errorLogs.addAll(bRes.errors);

                currentBatch.clear();
                log.info("Import Progress: {}/{} processed (Inserted: {}, Updated: {}, Skipped: {}, Failed: {})",
                        processed, targetFiles.size(), inserted, updated, skipped, failed);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Phase 2B Import [{}] Completed in {} ms. Inserted: {}, Updated: {}, Skipped: {}, Failed: {}",
                mode, duration, inserted, updated, skipped, failed);

        if (!errorLogs.isEmpty()) {
            saveErrorLog(errorLogs);
        }

        return MySchemeImportReport.builder()
                .mode(mode)
                .totalFilesFound(totalFound)
                .processedCount(processed)
                .insertedCount(inserted)
                .updatedCount(updated)
                .skippedCount(skipped)
                .failedCount(failed)
                .executionTimeMs(duration)
                .status(failed == 0 ? "SUCCESS" : "COMPLETED_WITH_ERRORS")
                .errors(errorLogs)
                .build();
    }

    private static class BatchResult {
        int processed = 0;
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
    }

    @Transactional
    public BatchResult processBatch(List<File> batchFiles) {
        BatchResult res = new BatchResult();
        for (File file : batchFiles) {
            res.processed++;
            try {
                JsonNode data = jsonParser.parseFile(file);
                if (data == null) {
                    res.failed++;
                    res.errors.add("Parse failed: " + file.getName());
                    continue;
                }

                String schemeId = data.has("_id") ? data.get("_id").asText() : null;
                String slug = data.has("slug") ? data.get("slug").asText() : null;

                if (schemeId == null || schemeId.isEmpty()) {
                    res.failed++;
                    res.errors.add("Missing _id: " + file.getName());
                    continue;
                }

                // Dynamic Category Resolution
                SchemeCategory category = resolveCategory(data);
                // Dynamic Department Resolution
                SchemeDepartment department = resolveDepartment(data);

                Scheme mapped = mapper.mapToScheme(data, category, department);
                if (mapped == null) {
                    res.failed++;
                    res.errors.add("Mapping returned null: " + file.getName());
                    continue;
                }

                // Check existing scheme for idempotency
                Optional<Scheme> existing = schemeRepository.findById(schemeId);
                if (existing.isEmpty() && slug != null) {
                    existing = schemeRepository.findBySlug(slug);
                }
                if (existing.isEmpty() && mapped.getSchemeCode() != null) {
                    existing = schemeRepository.findBySchemeCode(mapped.getSchemeCode());
                }

                if (existing.isPresent()) {
                    // Update existing
                    Scheme s = existing.get();
                    updateSchemeFields(s, mapped);
                    schemeRepository.save(s);
                    res.updated++;
                } else {
                    // Insert new
                    schemeRepository.save(mapped);
                    res.inserted++;
                }

            } catch (Exception e) {
                res.failed++;
                res.errors.add("Error processing " + file.getName() + ": " + e.getMessage());
                log.error("Error processing file {}: {}", file.getName(), e.getMessage(), e);
            }
        }
        return res;
    }

    private void updateSchemeFields(Scheme target, Scheme source) {
        target.setSchemeCode(source.getSchemeCode());
        target.setSlug(source.getSlug());
        target.setTitleEnglish(source.getTitleEnglish());
        target.setDescriptionEnglish(source.getDescriptionEnglish());
        target.setDetailedDescription(source.getDetailedDescription());
        target.setEligibilityText(source.getEligibilityText());
        target.setApplicationProcess(source.getApplicationProcess());
        target.setCategory(source.getCategory());
        target.setDepartment(source.getDepartment());
        target.setSchemeType(source.getSchemeType());
        target.setApplicableStates(source.getApplicableStates());
        target.setDbtScheme(source.getDbtScheme());
        target.setTargetBeneficiaries(source.getTargetBeneficiaries());
        target.setStatus("ACTIVE");

        // Benefits child collection update
        target.getBenefits().clear();
        if (source.getBenefits() != null) {
            for (SchemeBenefit b : source.getBenefits()) {
                b.setScheme(target);
                target.getBenefits().add(b);
            }
        }

        // Tags child collection update
        target.getTags().clear();
        if (source.getTags() != null) {
            for (SchemeTag t : source.getTags()) {
                t.setScheme(target);
                target.getTags().add(t);
            }
        }
    }

    @Transactional
    public SchemeCategory resolveCategory(JsonNode data) {
        JsonNode en = data.has("en") ? data.get("en") : null;
        JsonNode basic = (en != null && en.has("basicDetails")) ? en.get("basicDetails") : null;

        String catName = "General Welfare";
        if (basic != null && basic.has("schemeCategory") && !basic.get("schemeCategory").isNull()) {
            JsonNode catNode = basic.get("schemeCategory");
            if (catNode.isArray() && catNode.size() > 0) {
                JsonNode first = catNode.get(0);
                catName = first.has("label") ? first.get("label").asText() : first.asText();
            } else if (catNode.isObject() && catNode.has("label")) {
                catName = catNode.get("label").asText();
            } else {
                catName = catNode.asText();
            }
        }

        catName = catName.trim();
        String catCode = catName.toUpperCase().replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_");
        if (catCode.length() > 40) catCode = catCode.substring(0, 40);

        if (categoryCache.containsKey(catCode)) {
            return categoryCache.get(catCode);
        }

        Optional<SchemeCategory> existing = categoryRepository.findByCode(catCode);
        if (existing.isPresent()) {
            categoryCache.put(catCode, existing.get());
            return existing.get();
        }

        SchemeCategory newCat = SchemeCategory.builder()
                .code(catCode)
                .name(catName)
                .nameEnglish(catName)
                .status("ACTIVE")
                .displayOrder(categoryCache.size() + 1)
                .build();

        SchemeCategory saved = categoryRepository.save(newCat);
        categoryCache.put(catCode, saved);
        return saved;
    }

    @Transactional
    public SchemeDepartment resolveDepartment(JsonNode data) {
        JsonNode en = data.has("en") ? data.get("en") : null;
        JsonNode basic = (en != null && en.has("basicDetails")) ? en.get("basicDetails") : null;

        String deptName = "";
        String ministryName = "";

        if (basic != null) {
            if (basic.has("nodalDepartmentName") && !basic.get("nodalDepartmentName").isNull()) {
                JsonNode dNode = basic.get("nodalDepartmentName");
                deptName = dNode.has("label") ? dNode.get("label").asText() : dNode.asText();
            }
            if (basic.has("nodalMinistryName") && !basic.get("nodalMinistryName").isNull()) {
                JsonNode mNode = basic.get("nodalMinistryName");
                ministryName = mNode.has("label") ? mNode.get("label").asText() : mNode.asText();
            }
        }

        String finalName = !deptName.isEmpty() ? deptName : (!ministryName.isEmpty() ? ministryName : "General Department");
        finalName = finalName.trim();

        String deptCode = finalName.toUpperCase().replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_");
        if (deptCode.length() > 40) deptCode = deptCode.substring(0, 40);

        if (departmentCache.containsKey(deptCode)) {
            return departmentCache.get(deptCode);
        }

        Optional<SchemeDepartment> existing = departmentRepository.findByCode(deptCode);
        if (existing.isPresent()) {
            departmentCache.put(deptCode, existing.get());
            return existing.get();
        }

        SchemeDepartment newDept = SchemeDepartment.builder()
                .code(deptCode)
                .name(finalName)
                .ministry(ministryName.isEmpty() ? null : ministryName)
                .status("ACTIVE")
                .build();

        SchemeDepartment saved = departmentRepository.save(newDept);
        departmentCache.put(deptCode, saved);
        return saved;
    }

    private void preloadCaches() {
        categoryRepository.findAll().forEach(c -> categoryCache.put(c.getCode(), c));
        departmentRepository.findAll().forEach(d -> departmentCache.put(d.getCode(), d));
    }

    private void saveErrorLog(List<String> errorLogs) {
        try {
            File logDir = new File("d:/schemeBridge/myscheme-data/logs");
            logDir.mkdirs();
            File errFile = new File(logDir, "phase2b-import-errors.json");
            ObjectMapper om = new ObjectMapper();
            om.writerWithDefaultPrettyPrinter().writeValue(errFile, errorLogs);
            log.info("Saved error log to {}", errFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to write error log file: {}", e.getMessage());
        }
    }
}
