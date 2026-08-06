package com.schemebridge.controller;

import com.schemebridge.common.ApiResponse;
import com.schemebridge.dto.AdminUserRoleRequest;
import com.schemebridge.dto.AdminUserStatusRequest;
import com.schemebridge.dto.AdminUserUpdateRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHEME_MANAGER', 'VERIFICATION_OFFICER')")
@Tag(name = "Admin User Management", description = "Administrative user management and privileged account updates")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Returns a paginated, searchable, filterable user management view")
    public ResponseEntity<ApiResponse<Page<UserDto>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            Pageable pageable) {
        Page<UserDto> users = adminUserService.listUsers(search, status, role, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user", description = "Retrieves a single user record for admin review")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable String id) {
        UserDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Updates admin-managed user profile information")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        UserDto user = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Update user status", description = "Updates the account status for an administrative user record")
    public ResponseEntity<ApiResponse<UserDto>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody AdminUserStatusRequest request) {
        UserDto user = adminUserService.updateUserStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Update user role", description = "Updates the privileged role assignment for a user")
    public ResponseEntity<ApiResponse<UserDto>> updateRole(
            @PathVariable String id,
            @Valid @RequestBody AdminUserRoleRequest request) {
        UserDto user = adminUserService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", user));
    }
}
