package com.schemebridge.config;

import com.schemebridge.entity.Role;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        log.info("Checking default roles in database...");
        Arrays.stream(RoleEnum.values()).forEach(roleEnum -> {
            if (!roleRepository.existsByName(roleEnum)) {
                Role role = Role.builder()
                        .name(roleEnum)
                        .build();
                roleRepository.save(role);
                log.info("Seeded default role: {}", roleEnum);
            }
        });
        log.info("Role initialization completed.");
    }
}
