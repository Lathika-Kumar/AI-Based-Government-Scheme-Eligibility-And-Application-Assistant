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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public PagedAdminUserResponse getUsers(String search, String statusStr, String roleStr, int page, int size, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"), sortField != null ? sortField : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), term);
                Predicate lastNameMatch  = cb.like(cb.lower(root.get("lastName")), term);
                Predicate fullNameMatch  = cb.like(cb.concat(cb.concat(cb.lower(root.get("firstName")), " "), cb.lower(root.get("lastName"))), term);
                Predicate emailMatch     = cb.like(cb.lower(root.get("email")), term);
                Predicate phoneMatch     = cb.like(cb.lower(root.get("phoneNumber")), term);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, fullNameMatch, emailMatch, phoneMatch));
            }

            if (statusStr != null && !statusStr.trim().isEmpty() && !"all".equalsIgnoreCase(statusStr)) {
                try {
                    AccountStatus status = AccountStatus.valueOf(statusStr.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("accountStatus"), status));
                } catch (IllegalArgumentException ignored) {}
            }

            if (roleStr != null && !roleStr.trim().isEmpty() && !"all".equalsIgnoreCase(roleStr)) {
                List<String> candidates = resolveRoleCandidates(roleStr);
                if (!candidates.isEmpty()) {
                    Join<User, Role> roleJoin = root.join("roles");
                    predicates.add(cb.upper(roleJoin.get("name")).in(candidates));
                }
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<AdminUserResponse> content = userPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        long totalUsers = userRepository.count();
        Map<String, Long> roleCounts = getRoleCounts();

        return PagedAdminUserResponse.builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .totalUsers(totalUsers)
                .roleCounts(roleCounts)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, String actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AccountStatus newStatus = request.getStatus();
        // Prevent deactivating/suspending the last active ADMIN
        boolean isUserAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName()));
        if (isUserAdmin && newStatus != AccountStatus.ACTIVE) {
            long activeAdminCount = userRepository.countByRoles_NameAndAccountStatus("ROLE_ADMIN", AccountStatus.ACTIVE)
                    + userRepository.countByRoles_NameAndAccountStatus("ADMIN", AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot deactivate the only active Administrator account.");
            }
        }

        user.setAccountStatus(newStatus);
        User saved = userRepository.save(user);
        log.info("Admin actor={} updated user status userId={} to {}", actorId, userId, newStatus);
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request, String actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Set<String> normalizedRoleNames = request.getRoles().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .collect(Collectors.toSet());

        boolean wasAdmin = user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName()));
        boolean willBeAdmin = normalizedRoleNames.contains("ROLE_ADMIN");

        if (wasAdmin && !willBeAdmin) {
            long activeAdminCount = userRepository.countByRoles_NameAndAccountStatus("ROLE_ADMIN", AccountStatus.ACTIVE)
                    + userRepository.countByRoles_NameAndAccountStatus("ADMIN", AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot remove ROLE_ADMIN from the only active Administrator account.");
            }
        }

        Set<Role> resolvedRoles = new HashSet<>();
        for (String roleName : normalizedRoleNames) {
            String unprefixed = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
            Role role = roleRepository.findByName(roleName)
                    .or(() -> roleRepository.findByName(unprefixed))
                    .or(() -> roleRepository.findByNameIgnoreCase(roleName))
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).description(roleName + " authority").build()));
            resolvedRoles.add(role);
        }

        user.setRoles(resolvedRoles);
        User saved = userRepository.save(user);
        log.info("Admin actor={} updated user roles userId={} to {}", actorId, userId, normalizedRoleNames);
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse unlockUser(Long userId, String actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setAccountStatus(AccountStatus.ACTIVE);
        User saved = userRepository.save(user);
        log.info("Admin actor={} unlocked user userId={}", actorId, userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRoleCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", userRepository.count());
        counts.put("citizens", userRepository.countUsersByRoleNames(List.of("USER", "ROLE_USER")));
        counts.put("officers", userRepository.countUsersByRoleNames(List.of("VERIFICATION_OFFICER", "ROLE_VERIFICATION_OFFICER")));
        counts.put("managers", userRepository.countUsersByRoleNames(List.of("SCHEME_MANAGER", "ROLE_SCHEME_MANAGER")));
        counts.put("admins", userRepository.countUsersByRoleNames(List.of("ADMIN", "ROLE_ADMIN", "SUPER_ADMIN", "ROLE_SUPER_ADMIN")));
        return counts;
    }

    private List<String> resolveRoleCandidates(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty() || "all".equalsIgnoreCase(roleStr.trim())) {
            return Collections.emptyList();
        }
        String clean = roleStr.trim().toUpperCase();
        if ("CITIZENS".equals(clean) || "CITIZEN".equals(clean) || "USER".equals(clean) || "ROLE_USER".equals(clean)) {
            return List.of("USER", "ROLE_USER");
        }
        if ("ADMIN".equals(clean) || "ROLE_ADMIN".equals(clean) || "ADMINS".equals(clean) || "ADMINISTRATOR".equals(clean)
                || "SUPER_ADMIN".equals(clean) || "ROLE_SUPER_ADMIN".equals(clean) || "SUPER_ADMINS".equals(clean)) {
            return List.of("ADMIN", "ROLE_ADMIN", "SUPER_ADMIN", "ROLE_SUPER_ADMIN");
        }
        if ("SCHEME_MANAGER".equals(clean) || "ROLE_SCHEME_MANAGER".equals(clean) || "MANAGERS".equals(clean) || "MANAGER".equals(clean)) {
            return List.of("SCHEME_MANAGER", "ROLE_SCHEME_MANAGER");
        }
        if ("VERIFICATION_OFFICER".equals(clean) || "ROLE_VERIFICATION_OFFICER".equals(clean) || "OFFICERS".equals(clean) || "OFFICER".equals(clean)) {
            return List.of("VERIFICATION_OFFICER", "ROLE_VERIFICATION_OFFICER");
        }
        String unprefixed = clean.startsWith("ROLE_") ? clean.substring(5) : clean;
        String prefixed = clean.startsWith("ROLE_") ? clean : "ROLE_" + clean;
        return List.of(clean, unprefixed, prefixed);
    }

    private AdminUserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name : "ROLE_" + name)
                .sorted()
                .collect(Collectors.toList());

        return AdminUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dob(user.getDob())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
