package com.schemebridge.service;

import com.schemebridge.dto.AdminUserRoleRequest;
import com.schemebridge.dto.AdminUserStatusRequest;
import com.schemebridge.dto.AdminUserUpdateRequest;
import com.schemebridge.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<UserDto> listUsers(String search, String status, String role, Pageable pageable);
    UserDto getUserById(String id);
    UserDto updateUser(String id, AdminUserUpdateRequest request);
    UserDto updateUserStatus(String id, AdminUserStatusRequest request);
    UserDto updateUserRole(String id, AdminUserRoleRequest request);
}
