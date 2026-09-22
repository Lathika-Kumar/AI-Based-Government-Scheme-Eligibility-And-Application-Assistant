package com.schemebridge.auth.controller;

import com.schemebridge.auth.entity.User;
import com.schemebridge.auth.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication Diagnostic", description = "Safe diagnostic endpoint for verifying Oracle cloud connectivity and schema health")
public class AuthDiagnosticController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @GetMapping("/diagnostic")
    @Operation(summary = "Safe database health diagnostic (zero credentials exposed)")
    public ResponseEntity<Map<String, Object>> getDiagnostic(
            @RequestParam(required = false, defaultValue = "admin@schemebridge.gov.in") String testEmail) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "schemebridge-auth-service");
        result.put("timestamp", new Date());

        // 1. Check Oracle connectivity
        try {
            Integer dummy = jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            result.put("oracleConnection", "CONNECTED");
        } catch (Exception e) {
            result.put("oracleConnection", "NOT_CONNECTED");
            result.put("oracleError", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.ok(result);
        }

        // 2. Check table record counts safely
        Map<String, Object> tableCounts = new LinkedHashMap<>();
        String[] tables = {"USERS", "ROLES", "USER_ROLES", "OTP_VERIFICATION", "REFRESH_TOKENS", "flyway_schema_history"};
        for (String table : tables) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                tableCounts.put(table, count);
            } catch (Exception e) {
                tableCounts.put(table, "ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        result.put("tableCounts", tableCounts);

        // 3. Check USERS table columns (especially DOB)
        try {
            List<String> columns = jdbcTemplate.query(
                    "SELECT COLUMN_NAME FROM USER_TAB_COLS WHERE TABLE_NAME = 'USERS' ORDER BY COLUMN_ID",
                    (rs, rowNum) -> rs.getString("COLUMN_NAME"));
            result.put("usersTableColumns", columns);
            result.put("hasDobColumn", columns.contains("DOB"));
        } catch (Exception e) {
            result.put("usersTableColumnsError", e.getMessage());
        }

        // 4. Check Flyway applied migrations
        try {
            List<Map<String, Object>> migrations = jdbcTemplate.query(
                    "SELECT \"version\", \"description\", \"type\", \"script\", \"success\" FROM \"flyway_schema_history\" ORDER BY \"installed_rank\"",
                    (rs, rowNum) -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("version", rs.getString("version"));
                        m.put("description", rs.getString("description"));
                        m.put("type", rs.getString("type"));
                        m.put("success", rs.getInt("success") == 1);
                        return m;
                    });
            result.put("flywayMigrations", migrations);
        } catch (Exception e) {
            // Try uppercase table name
            try {
                List<Map<String, Object>> migrations = jdbcTemplate.query(
                        "SELECT VERSION, DESCRIPTION, TYPE, SCRIPT, SUCCESS FROM FLYWAY_SCHEMA_HISTORY ORDER BY INSTALLED_RANK",
                        (rs, rowNum) -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("version", rs.getString("VERSION"));
                            m.put("description", rs.getString("DESCRIPTION"));
                            m.put("type", rs.getString("TYPE"));
                            m.put("success", rs.getInt("SUCCESS") == 1);
                            return m;
                        });
                result.put("flywayMigrations", migrations);
            } catch (Exception ex2) {
                result.put("flywayMigrationsError", ex2.getMessage());
            }
        }

        // 5. Test JPA findByEmail execution
        Map<String, Object> jpaTest = new LinkedHashMap<>();
        try {
            Optional<User> userOpt = userRepository.findByEmail(testEmail.trim().toLowerCase());
            jpaTest.put("querySuccess", true);
            jpaTest.put("userFound", userOpt.isPresent());
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                jpaTest.put("userId", u.getId());
                jpaTest.put("email", u.getEmail());
                jpaTest.put("accountStatus", u.getAccountStatus() != null ? u.getAccountStatus().name() : null);
                jpaTest.put("emailVerified", u.isEmailVerified());
                jpaTest.put("hasPasswordHash", u.getPasswordHash() != null && !u.getPasswordHash().isBlank());
                jpaTest.put("rolesCount", u.getRoles() != null ? u.getRoles().size() : 0);
                List<String> roleNames = new ArrayList<>();
                if (u.getRoles() != null) {
                    u.getRoles().forEach(r -> roleNames.add(r.getName()));
                }
                jpaTest.put("roles", roleNames);
            }
        } catch (Exception e) {
            jpaTest.put("querySuccess", false);
            jpaTest.put("errorClass", e.getClass().getName());
            jpaTest.put("errorMessage", e.getMessage());
            if (e.getCause() != null) {
                jpaTest.put("causeClass", e.getCause().getClass().getName());
                jpaTest.put("causeMessage", e.getCause().getMessage());
            }
        }
        result.put("jpaTest", jpaTest);

        return ResponseEntity.ok(result);
    }
}
