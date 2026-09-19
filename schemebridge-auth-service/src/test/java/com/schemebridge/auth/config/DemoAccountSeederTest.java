package com.schemebridge.auth.config;

import com.schemebridge.auth.entity.AccountStatus;
import com.schemebridge.auth.entity.Role;
import com.schemebridge.auth.entity.User;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoAccountSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DemoAccountSeeder demoAccountSeeder;

    private Role voRole;
    private Role smRole;

    @BeforeEach
    void setUp() {
        voRole = Role.builder().id(3L).name("VERIFICATION_OFFICER").description("Government verification officer").build();
        smRole = Role.builder().id(4L).name("SCHEME_MANAGER").description("Scheme administrator and manager").build();

        ReflectionTestUtils.setField(demoAccountSeeder, "seedAccountsEnabled", true);
    }

    @Test
    void testRun_CreatesBothAccountsWhenNotPresent() {
        when(roleRepository.findByName("VERIFICATION_OFFICER")).thenReturn(Optional.of(voRole));
        when(roleRepository.findByName("SCHEME_MANAGER")).thenReturn(Optional.of(smRole));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Verify@12345")).thenReturn("$2a$10$hashedVO");
        when(passwordEncoder.encode("Scheme@12345")).thenReturn("$2a$10$hashedSM");

        demoAccountSeeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        var savedUsers = userCaptor.getAllValues();
        assertEquals(2, savedUsers.size());

        User voUser = savedUsers.stream()
                .filter(u -> u.getEmail().equals("verification.officer@schemebridge.gov.in"))
                .findFirst().orElseThrow();
        assertEquals("Verification", voUser.getFirstName());
        assertEquals("Officer", voUser.getLastName());
        assertEquals(AccountStatus.ACTIVE, voUser.getAccountStatus());
        assertTrue(voUser.isEmailVerified());
        assertEquals(1, voUser.getRoles().size());
        assertTrue(voUser.getRoles().contains(voRole));
        assertFalse(voUser.getRoles().stream().anyMatch(r -> r.getName().contains("ADMIN")));

        User smUser = savedUsers.stream()
                .filter(u -> u.getEmail().equals("scheme.manager@schemebridge.gov.in"))
                .findFirst().orElseThrow();
        assertEquals("Scheme", smUser.getFirstName());
        assertEquals("Manager", smUser.getLastName());
        assertEquals(AccountStatus.ACTIVE, smUser.getAccountStatus());
        assertTrue(smUser.isEmailVerified());
        assertEquals(1, smUser.getRoles().size());
        assertTrue(smUser.getRoles().contains(smRole));
        assertFalse(smUser.getRoles().stream().anyMatch(r -> r.getName().contains("ADMIN")));
    }

    @Test
    void testRun_IdempotentWhenAlreadyPresentAndMatching() {
        when(roleRepository.findByName("VERIFICATION_OFFICER")).thenReturn(Optional.of(voRole));
        when(roleRepository.findByName("SCHEME_MANAGER")).thenReturn(Optional.of(smRole));

        User existingVo = User.builder()
                .id(101L)
                .email("verification.officer@schemebridge.gov.in")
                .firstName("Verification")
                .lastName("Officer")
                .passwordHash("$2a$10$hashedVO")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(voRole))
                .build();

        User existingSm = User.builder()
                .id(102L)
                .email("scheme.manager@schemebridge.gov.in")
                .firstName("Scheme")
                .lastName("Manager")
                .passwordHash("$2a$10$hashedSM")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(smRole))
                .build();

        when(userRepository.findByEmail("verification.officer@schemebridge.gov.in")).thenReturn(Optional.of(existingVo));
        when(userRepository.findByEmail("scheme.manager@schemebridge.gov.in")).thenReturn(Optional.of(existingSm));
        when(passwordEncoder.matches("Verify@12345", "$2a$10$hashedVO")).thenReturn(true);
        when(passwordEncoder.matches("Scheme@12345", "$2a$10$hashedSM")).thenReturn(true);

        demoAccountSeeder.run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRun_DisabledWhenFlagIsFalse() {
        ReflectionTestUtils.setField(demoAccountSeeder, "seedAccountsEnabled", false);

        demoAccountSeeder.run(new DefaultApplicationArguments());

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any());
    }
}
