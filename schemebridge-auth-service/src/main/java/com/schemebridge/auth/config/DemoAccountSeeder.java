package com.schemebridge.auth.config;

import com.schemebridge.auth.entity.AccountStatus;
import com.schemebridge.auth.entity.Role;
import com.schemebridge.auth.entity.User;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * DEMO / SANDBOX ONLY Seeder for Verification Officer and Scheme Manager accounts.
 *
 * Production Safety Protocol:
 * 1. Strict profile guard: Active on non-production profiles only (!prod & !production).
 * 2. Explicit property flag: app.demo.seed-accounts.enabled (can be set to false).
 * 3. Idempotent: safe to run repeatedly, never duplicates accounts, strictly enforces single canonical role.
 */
@Component
@Order(10)
@Profile("!prod & !production")
@RequiredArgsConstructor
@Slf4j
public class DemoAccountSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.seed-accounts.enabled:true}")
    private boolean seedAccountsEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedAccountsEnabled) {
            log.info("[DEMO SEEDER] Demo account seeding is disabled (app.demo.seed-accounts.enabled=false).");
            return;
        }

        log.info("[DEMO SEEDER] Checking and seeding DEMO / SANDBOX accounts...");

        Role voRole = roleRepository.findByName("VERIFICATION_OFFICER")
                .or(() -> roleRepository.findByName("ROLE_VERIFICATION_OFFICER"))
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("VERIFICATION_OFFICER")
                        .description("Government verification officer")
                        .build()));

        Role smRole = roleRepository.findByName("SCHEME_MANAGER")
                .or(() -> roleRepository.findByName("ROLE_SCHEME_MANAGER"))
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("SCHEME_MANAGER")
                        .description("Scheme administrator and manager")
                        .build()));

        // 1. Verification Officer (DEMO ONLY)
        seedDemoAccount(
                "verification.officer@schemebridge.gov.in",
                "Verify@12345",
                "Verification",
                "Officer",
                "9876543202",
                voRole
        );

        // 2. Scheme Manager (DEMO ONLY)
        seedDemoAccount(
                "scheme.manager@schemebridge.gov.in",
                "Scheme@12345",
                "Scheme",
                "Manager",
                "9876543203",
                smRole
        );
    }

    private void seedDemoAccount(String email, String rawPassword, String firstName, String lastName, String phone, Role exclusiveRole) {
        Optional<User> existingOpt = userRepository.findByEmail(email);
        if (existingOpt.isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .accountStatus(AccountStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(Set.of(exclusiveRole))
                    .build();
            userRepository.save(user);
            log.info("[DEMO SEEDER] Created new DEMO user: {} with exclusive role: {}", email, exclusiveRole.getName());
        } else {
            User user = existingOpt.get();
            boolean updated = false;

            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                updated = true;
            }
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                user.setAccountStatus(AccountStatus.ACTIVE);
                updated = true;
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                updated = true;
            }
            // Strict single role assignment — remove any accidental ADMIN or other roles
            if (user.getRoles().size() != 1 || !user.getRoles().contains(exclusiveRole)) {
                user.setRoles(Set.of(exclusiveRole));
                updated = true;
            }
            if (!firstName.equals(user.getFirstName()) || !lastName.equals(user.getLastName())) {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
                log.info("[DEMO SEEDER] Synchronized existing DEMO user: {} with exclusive role: {}", email, exclusiveRole.getName());
            } else {
                log.info("[DEMO SEEDER] DEMO user {} already up to date.", email);
            }
        }
    }
}
