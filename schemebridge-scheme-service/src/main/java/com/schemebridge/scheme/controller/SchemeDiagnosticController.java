package com.schemebridge.scheme.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/schemes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scheme Diagnostic", description = "Safe diagnostic endpoint for verifying MongoDB cloud connectivity and collection document counts")
public class SchemeDiagnosticController {

    private final MongoTemplate mongoTemplate;

    private static final String[] COLLECTIONS = {
            "schemes",
            "scheme_categories",
            "scheme_verified_data",
            "applications",
            "application_documents",
            "application_reviews",
            "application_events",
            "admin_audit_logs",
            "admin_settings",
            "citizen_profiles",
            "citizen_vault_documents",
            "document_verification_results",
            "grievances",
            "notifications",
            "portal_feedback",
            "recommendation_events",
            "database_sequences",
            "fs.files",
            "fs.chunks"
    };

    @GetMapping("/diagnostic")
    @Operation(summary = "Safe database health diagnostic (zero credentials exposed)")
    public ResponseEntity<Map<String, Object>> getDiagnostic() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "schemebridge-scheme-service");
        result.put("timestamp", new Date());

        try {
            Document pingResult = mongoTemplate.getDb().runCommand(new Document("ping", 1));
            result.put("mongoConnection", "CONNECTED");
            result.put("databaseName", mongoTemplate.getDb().getName());
        } catch (Exception e) {
            result.put("mongoConnection", "NOT_CONNECTED");
            result.put("mongoError", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.ok(result);
        }

        Map<String, Object> collectionCounts = new LinkedHashMap<>();
        Set<String> existingNames = new HashSet<>();
        try {
            for (String name : mongoTemplate.getDb().listCollectionNames()) {
                existingNames.add(name);
            }
        } catch (Exception e) {
            log.warn("Failed to list collection names: {}", e.getMessage());
        }

        for (String col : COLLECTIONS) {
            try {
                if (existingNames.contains(col)) {
                    long count = mongoTemplate.getCollection(col).countDocuments();
                    collectionCounts.put(col, count);
                } else {
                    collectionCounts.put(col, 0L);
                }
            } catch (Exception e) {
                collectionCounts.put(col, "ERROR: " + e.getMessage());
            }
        }
        result.put("collections", collectionCounts);

        return ResponseEntity.ok(result);
    }
}
