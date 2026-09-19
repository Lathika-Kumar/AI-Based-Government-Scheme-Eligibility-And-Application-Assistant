package com.schemebridge.auth.service;

import com.schemebridge.auth.dto.request.UpdateUserRolesRequest;
import com.schemebridge.auth.dto.request.UpdateUserStatusRequest;
import com.schemebridge.auth.dto.response.AdminUserResponse;
import com.schemebridge.auth.dto.response.PagedAdminUserResponse;
import com.schemebridge.auth.entity.AccountStatus;
import com.schemebridge.auth.entity.Role;
import com.schemebridge.auth.entity.User;
import com.schemebridge.auth.exception.ResourceNotFoundException;
import com.schemebridge.auth.repository.RoleRepository;
import com.schemebridge.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User testUser;
    private Role roleAdmin;
    private Role roleUser;

    @BeforeEach
    void setUp() {
        roleAdmin = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin").build();
        roleUser = Role.builder().id(2L).name("ROLE_USER").description("User").build();

        Set<Role> roles = new HashSet<>();
        roles.add(roleAdmin);

        testUser = User.builder()
                .id(100L)
                .firstName("Admin")
                .lastName("User")
                .email("admin@schemebridge.gov.in")
                .phoneNumber("9876543210")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetUsers_Success() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userRepository.count()).thenReturn(41L);
        when(userRepository.countUsersByRoleNames(any())).thenReturn(1L);

        PagedAdminUserResponse response = adminUserService.getUsers("admin", "ACTIVE", "ADMIN", 0, 10, "createdAt", "DESC");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("admin@schemebridge.gov.in", response.getContent().get(0).getEmail());
        assertEquals(41L, response.getTotalUsers());
        assertNotNull(response.getRoleCounts());
        assertEquals(41L, response.getRoleCounts().get("all"));
        assertTrue(response.getContent().get(0).getRoles().contains("ROLE_ADMIN"));
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        AdminUserResponse response = adminUserService.getUserById(100L);
        assertNotNull(response);
        assertEquals("admin@schemebridge.gov.in", response.getEmail());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminUserService.getUserById(999L));
    }

    @Test
    void testUpdateUserStatus_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.countByRoles_NameAndAccountStatus(eq("ROLE_ADMIN"), eq(AccountStatus.ACTIVE))).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AdminUserResponse response = adminUserService.updateUserStatus(100L, UpdateUserStatusRequest.builder().status(AccountStatus.LOCKED).build(), "1");
        assertNotNull(response);
        assertEquals(AccountStatus.LOCKED, testUser.getAccountStatus());
    }

    @Test
    void testUpdateUserStatus_PreventDeactivatingLastAdmin() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.countByRoles_NameAndAccountStatus(eq("ROLE_ADMIN"), eq(AccountStatus.ACTIVE))).thenReturn(1L);
        when(userRepository.countByRoles_NameAndAccountStatus(eq("ADMIN"), eq(AccountStatus.ACTIVE))).thenReturn(0L);

        assertThrows(IllegalStateException.class, () ->
                adminUserService.updateUserStatus(100L, UpdateUserStatusRequest.builder().status(AccountStatus.INACTIVE).build(), "1"));
    }


    @Test
    void testUpdateUserRoles_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            return Optional.of(Role.builder().name(name).description(name + " authority").build());
        });
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AdminUserResponse response = adminUserService.updateUserRoles(100L, UpdateUserRolesRequest.builder().roles(Set.of("ROLE_SCHEME_MANAGER", "ROLE_ADMIN")).build(), "1");
        assertNotNull(response);
    }

    @Test
    void testGetRoleCounts_Authoritative() {
        when(userRepository.count()).thenReturn(41L);
        when(userRepository.countUsersByRoleNames(List.of("USER", "ROLE_USER"))).thenReturn(40L);
        when(userRepository.countUsersByRoleNames(List.of("VERIFICATION_OFFICER", "ROLE_VERIFICATION_OFFICER"))).thenReturn(0L);
        when(userRepository.countUsersByRoleNames(List.of("SCHEME_MANAGER", "ROLE_SCHEME_MANAGER"))).thenReturn(1L);
        when(userRepository.countUsersByRoleNames(List.of("ADMIN", "ROLE_ADMIN", "SUPER_ADMIN", "ROLE_SUPER_ADMIN"))).thenReturn(2L);

        Map<String, Long> counts = adminUserService.getRoleCounts();

        assertNotNull(counts);
        assertEquals(41L, counts.get("all"));
        assertEquals(40L, counts.get("citizens"));
        assertEquals(0L, counts.get("officers"));
        assertEquals(1L, counts.get("managers"));
        assertEquals(2L, counts.get("admins"));
    }

    @Test
    void testRoleNormalization_UnprefixedRoleInDb() {
        Role rawAdmin = Role.builder().id(10L).name("ADMIN").description("Admin").build();
        User userWithRawRole = User.builder()
                .id(200L)
                .email("rawadmin@schemebridge.gov.in")
                .accountStatus(AccountStatus.ACTIVE)
                .roles(Set.of(rawAdmin))
                .build();

        when(userRepository.findById(200L)).thenReturn(Optional.of(userWithRawRole));

        AdminUserResponse response = adminUserService.getUserById(200L);
        assertNotNull(response);
        assertTrue(response.getRoles().contains("ROLE_ADMIN"), "Role should be normalized with ROLE_ prefix for API consumers");
    }
}
