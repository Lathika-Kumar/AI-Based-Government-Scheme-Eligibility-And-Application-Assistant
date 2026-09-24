package com.schemebridge.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class AuthApplication {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
        // Load local and parent .env file if they exist
        try {
            Path[] candidatePaths = {
                Paths.get("../.env"),
                Paths.get(".env")
            };
            for (Path envPath : candidatePaths) {
                if (Files.exists(envPath)) {
                    try (BufferedReader reader = Files.newBufferedReader(envPath)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            int equalIdx = line.indexOf('=');
                            if (equalIdx > 0) {
                                String key = line.substring(0, equalIdx).trim();
                                String value = line.substring(equalIdx + 1).trim();
                                // Remove surrounding quotes if any
                                if ((value.startsWith("\"") && value.endsWith("\"")) || 
                                    (value.startsWith("'") && value.endsWith("'"))) {
                                    value = value.substring(1, value.length() - 1);
                                }
                                System.setProperty(key, value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("WARN: Failed to load .env file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (System.getenv("JWT_EXPIRATION") == null && System.getProperty("JWT_EXPIRATION") == null) {
            System.setProperty("JWT_EXPIRATION", "3600000");
        }

        // Fail-fast configuration check
        String[] requiredKeys = {"ORACLE_URL", "ORACLE_USERNAME", "ORACLE_PASSWORD", "JWT_SECRET"};
        for (String key : requiredKeys) {
            String value = System.getenv(key);
            if (value == null) {
                value = System.getProperty(key);
            }
            if (value == null || value.trim().isEmpty() || value.contains("${")) {
                System.err.println("==========================================================================");
                System.err.println("FATAL CONFIGURATION ERROR: Missing or unresolved variable '" + key + "'");
                System.err.println("Please make sure it is configured in your environment or in a local '.env' file.");
                System.err.println("==========================================================================");
                System.exit(1);
            }
        }

        SpringApplication.run(AuthApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.repair();
            } catch (Exception e) {
                System.err.println("WARN: Flyway repair warning: " + e.getMessage());
            }
            flyway.migrate();
        };
    }
}
