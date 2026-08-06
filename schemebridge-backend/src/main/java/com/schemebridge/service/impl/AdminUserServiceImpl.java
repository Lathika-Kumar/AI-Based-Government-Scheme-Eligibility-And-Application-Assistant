package com.schemebridge.service.impl;

import com.schemebridge.dto.AdminUserRoleRequest;
import com.schemebridge.dto.AdminUserStatusRequest;
import com.schemebridge.dto.AdminUserUpdateRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.entity.User;
import com.schemebridge.enums.AccountStatus;
import com.schemebridge.enums.RoleEnum;
import com.schemebridge.exception.BadRequestException;
import com.schemebridge.exception.ResourceNotFoundException;
import com.schemebridge.mapper.UserMapper;
import com.schemebridge.repository.UserRepository;
import com.schemebridge.service.AdminUserService;
import com.schemebridge.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    public Page<UserDto> listUsers(String search, String status, String role, Pageable pageable) {
        Query query = new Query();

        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("email").regex(search.trim(), "i"),
                    Criteria.where("fullName").regex(search.trim(), "i"),
                    Criteria.where("phoneNumber").regex(search.trim(), "i")
            ));
        }

        if (status != null && !status.isBlank()) {
            query.addCriteria(Criteria.where("status").is(AccountStatus.valueOf(status.trim().toUpperCase())));
        }

        if (role != null && !role.isBlank()) {
            query.addCriteria(Criteria.where("roles").all(RoleEnum.valueOf(role.trim().toUpperCase())));
        }

        long total = mongoTemplate.count(query, User.class);
        query.with(pageable);
        List<User> users = mongoTemplate.find(query, User.class);
        List<UserDto> dtos = users.stream().map(userMapper::toUserDto).toList();
        return new PageImpl<>(dtos, pageable, total);
    }

    @Override
    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserDto(user);
    }

    @Override
    public UserDto updateUser(String id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getRoles() != null) {
            user.setRoles(request.getRoles());
        }

        User saved = userRepository.save(user);
        return userMapper.toUserDto(saved);
    }

    @Override
    public UserDto updateUserStatus(String id, AdminUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required.");
        }
        AccountStatus previousStatus = user.getStatus();
        user.setStatus(request.getStatus());
        User saved = userRepository.save(user);

        if (previousStatus != AccountStatus.ACTIVE && request.getStatus() == AccountStatus.ACTIVE) {
            notificationService.sendAccountApproved(saved);
        } else if (previousStatus != AccountStatus.SUSPENDED && request.getStatus() == AccountStatus.SUSPENDED) {
            notificationService.sendAccountRejected(saved, "Your account has been suspended by the administrator.");
        }

        return userMapper.toUserDto(saved);
    }

    @Override
    public UserDto updateUserRole(String id, AdminUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (request.getRole() == null) {
            throw new BadRequestException("Role is required.");
        }
        user.getRoles().clear();
        user.getRoles().add(request.getRole());
        User saved = userRepository.save(user);
        return userMapper.toUserDto(saved);
    }
}
