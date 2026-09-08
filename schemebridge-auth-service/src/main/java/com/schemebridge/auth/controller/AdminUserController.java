package com.schemebridge.auth.controller;

import com.schemebridge.auth.dto.request.UpdateUserRolesRequest;
import com.schemebridge.auth.dto.request.UpdateUserStatusRequest;
import com.schemebridge.auth.dto.response.AdminUserResponse;
import com.schemebridge.auth.dto.response.PagedAdminUserResponse;
import com.schemebridge.auth.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Administrative endpoints for managing users, roles, and account status")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    private String getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @GetMapping
    @Operation(summary = "List users with pagination, search, status, and role filters (Admin only)")
    public ResponseEntity<PagedAdminUserResponse> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedAdminUserResponse response = adminUserService.getUsers(search, status, role, page, size, sort, direction);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user details by ID (Admin only)")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long userId) {
        AdminUserResponse response = adminUserService.getUserById(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update account status (ACTIVE, SUSPENDED, DEACTIVATED) (Admin only)")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        String actorId = getActorId();
        AdminUserResponse response = adminUserService.updateUserStatus(userId, request, actorId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{userId}/roles")
    @Operation(summary = "Update user assigned roles (Admin only)")
    public ResponseEntity<AdminUserResponse> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        String actorId = getActorId();
        AdminUserResponse response = adminUserService.updateUserRoles(userId, request, actorId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock/reactivate a suspended user account (Admin only)")
    public ResponseEntity<AdminUserResponse> unlockUser(@PathVariable Long userId) {
        String actorId = getActorId();
        AdminUserResponse response = adminUserService.unlockUser(userId, actorId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
