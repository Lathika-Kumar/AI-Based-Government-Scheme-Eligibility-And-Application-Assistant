package com.schemebridge.auth.config;

import com.schemebridge.auth.dto.request.LoginRequest;
import com.schemebridge.auth.dto.response.AdminUserResponse;
import com.schemebridge.auth.dto.response.LoginResponse;
import com.schemebridge.auth.dto.response.PagedAdminUserResponse;
import com.schemebridge.auth.entity.AccountStatus;
import com.schemebridge.auth.entity.Role;
import com.schemebridge.auth.entity.User;
import com.schemebridge.auth.exception.LoginVerificationException;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import com.schemebridge.auth.service.AdminUserService;
import com.schemebridge.auth.service.AuthService;
import com.schemebridge.auth.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live Oracle 21c Database & RBAC Integration Test Suite for DEMO Accounts.
 * Validates:
 * 1. Seeded DEMO accounts in Oracle 21c (USERS, ROLES, USER_ROLES tables).
 * 2. Strict exclusive role assignments (no accidental ADMIN privileges).
 * 3. Idempotent seeding (no duplicate records).
 * 4. PasswordEncoder BCrypt verification with correct & incorrect passwords.
 * 5. Admin directory dynamic aggregation (totalUsers=43, role counts).
 * 6. Exclusion of sensitive fields (password, passwordHash) from API serialization.
 */
@SpringBootTest
public class DemoAccountVerificationIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DemoAccountSeeder demoAccountSeeder;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    private static final String VO_EMAIL = "verification.officer@schemebridge.gov.in";
    private static final String SM_EMAIL = "scheme.manager@schemebridge.gov.in";

    @Test
    @DisplayName("1. Direct Oracle Database Verification: USERS, ROLES, USER_ROLES")
    void testDirectOracleDatabaseVerification() {
        // Query USERS table directly via JDBC
        List<Map<String, Object>> voUserRows = jdbcTemplate.queryForList(
                "SELECT id, email, first_name, last_name, account_status, email_verified FROM users WHERE email = ?",
                VO_EMAIL
        );
        assertEquals(1, voUserRows.size(), "Verification Officer must exist exactly once in Oracle USERS table");
        Map<String, Object> voRow = voUserRows.get(0);
        assertEquals("ACTIVE", String.valueOf(voRow.get("ACCOUNT_STATUS")));
        assertTrue(voRow.get("EMAIL_VERIFIED").toString().equals("1") || voRow.get("EMAIL_VERIFIED").toString().equalsIgnoreCase("true"));

        List<Map<String, Object>> smUserRows = jdbcTemplate.queryForList(
                "SELECT id, email, first_name, last_name, account_status, email_verified FROM users WHERE email = ?",
                SM_EMAIL
        );
        assertEquals(1, smUserRows.size(), "Scheme Manager must exist exactly once in Oracle USERS table");
        Map<String, Object> smRow = smUserRows.get(0);
        assertEquals("ACTIVE", String.valueOf(smRow.get("ACCOUNT_STATUS")));
        assertTrue(smRow.get("EMAIL_VERIFIED").toString().equals("1") || smRow.get("EMAIL_VERIFIED").toString().equalsIgnoreCase("true"));

        // Query USER_ROLES and ROLES joins directly via JDBC
        List<String> voRoles = jdbcTemplate.queryForList(
                "SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id JOIN users u ON ur.user_id = u.id WHERE u.email = ?",
                String.class,
                VO_EMAIL
        );
        assertEquals(1, voRoles.size(), "Verification Officer must have exactly 1 role");
        assertTrue(voRoles.contains("VERIFICATION_OFFICER") || voRoles.contains("ROLE_VERIFICATION_OFFICER"));
        assertFalse(voRoles.stream().anyMatch(r -> r.contains("ADMIN")), "Verification Officer must NOT have ADMIN role");

        List<String> smRoles = jdbcTemplate.queryForList(
                "SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id JOIN users u ON ur.user_id = u.id WHERE u.email = ?",
                String.class,
                SM_EMAIL
        );
        assertEquals(1, smRoles.size(), "Scheme Manager must have exactly 1 role");
        assertTrue(smRoles.contains("SCHEME_MANAGER") || smRoles.contains("ROLE_SCHEME_MANAGER"));
        assertFalse(smRoles.stream().anyMatch(r -> r.contains("ADMIN")), "Scheme Manager must NOT have ADMIN role");
    }

    @Test
    @DisplayName("2. Authentication: Correct password login succeeds with active status & valid JWT")
    void testDemoAccountLoginSuccess() {
        // Verification Officer
        LoginResponse voResponse = authService.login(LoginRequest.builder()
                .email(VO_EMAIL)
                .password("Verify@12345")
                .build());
        assertNotNull(voResponse);
        assertNotNull(voResponse.getAccessToken());
        assertEquals("Bearer", voResponse.getTokenType());
        assertEquals(VO_EMAIL, voResponse.getUser().getEmail());
        assertTrue(voResponse.getUser().getRoles().contains("VERIFICATION_OFFICER")
                || voResponse.getUser().getRoles().contains("ROLE_VERIFICATION_OFFICER"));
        assertFalse(voResponse.getUser().getRoles().stream().anyMatch(r -> r.contains("ADMIN")));

        // Scheme Manager
        LoginResponse smResponse = authService.login(LoginRequest.builder()
                .email(SM_EMAIL)
                .password("Scheme@12345")
                .build());
        assertNotNull(smResponse);
        assertNotNull(smResponse.getAccessToken());
        assertEquals("Bearer", smResponse.getTokenType());
        assertEquals(SM_EMAIL, smResponse.getUser().getEmail());
        assertTrue(smResponse.getUser().getRoles().contains("SCHEME_MANAGER")
                || smResponse.getUser().getRoles().contains("ROLE_SCHEME_MANAGER"));
        assertFalse(smResponse.getUser().getRoles().stream().anyMatch(r -> r.contains("ADMIN")));
    }

    @Test
    @DisplayName("3. Authentication: Incorrect password rejected")
    void testDemoAccountLoginFailure() {
        assertThrows(LoginVerificationException.class, () ->
                authService.login(LoginRequest.builder()
                        .email(VO_EMAIL)
                        .password("WrongPassword123!")
                        .build()));

        assertThrows(LoginVerificationException.class, () ->
                authService.login(LoginRequest.builder()
                        .email(SM_EMAIL)
                        .password("WrongPassword123!")
                        .build()));
    }

    @Test
    @DisplayName("4. Seeding Idempotency: Re-running seeder does not create duplicates or corrupt state")
    void testSeedingIdempotency() {
        long userCountBefore = userRepository.count();

        // Run seeder again
        demoAccountSeeder.run(new DefaultApplicationArguments());

        long userCountAfter = userRepository.count();
        assertEquals(userCountBefore, userCountAfter, "Idempotent seeder must not create duplicate users");

        // Verify accounts remain intact and single-role
        Optional<User> voOpt = userRepository.findByEmail(VO_EMAIL);
        assertTrue(voOpt.isPresent());
        assertEquals(1, voOpt.get().getRoles().size());

        Optional<User> smOpt = userRepository.findByEmail(SM_EMAIL);
        assertTrue(smOpt.isPresent());
        assertEquals(1, smOpt.get().getRoles().size());
    }

    @Test
    @DisplayName("5. Admin Directory Live Counts: totalUsers=43 with correct role distribution")
    void testAdminDirectoryCounts() {
        PagedAdminUserResponse response = adminUserService.getUsers(null, null, null, 0, 100, "createdAt", "DESC");
        assertNotNull(response);
        assertEquals(43, response.getTotalUsers(), "Total users must be 43");
        assertEquals(43, response.getTotalElements(), "Total elements on unpaginated/100-page must be 43");

        Map<String, Long> counts = response.getRoleCounts();
        assertNotNull(counts);
        assertEquals(43L, counts.get("all"));
        assertEquals(40L, counts.get("citizens"));
        assertEquals(1L, counts.get("officers"));
        assertEquals(2L, counts.get("managers"));
        assertEquals(2L, counts.get("admins"));
    }

    @Test
    @DisplayName("6. Security: Password & passwordHash fields never serialized in responses")
    void testSensitiveFieldsNeverSerialized() throws Exception {
        PagedAdminUserResponse pagedResponse = adminUserService.getUsers(null, null, null, 0, 10, "createdAt", "DESC");
        assertFalse(pagedResponse.getContent().isEmpty());

        String json = objectMapper.writeValueAsString(pagedResponse);

        assertFalse(json.contains("passwordHash"), "JSON must not contain passwordHash");
        assertFalse(json.contains("Verify@12345"), "JSON must not contain demo passwords");
        assertFalse(json.contains("Scheme@12345"), "JSON must not contain demo passwords");
    }
}
