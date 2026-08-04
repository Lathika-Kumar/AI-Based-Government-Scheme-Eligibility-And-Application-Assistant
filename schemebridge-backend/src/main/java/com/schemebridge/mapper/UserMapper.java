package com.schemebridge.mapper;

import com.schemebridge.dto.UserDto;
import com.schemebridge.entity.User;
import com.schemebridge.enums.RoleEnum;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }

        Set<RoleEnum> roles = user.getRoles() != null ? user.getRoles() : Set.of();

        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.getEnabled())
                .verificationMethod(user.getVerificationMethod())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .onboardingCompleted(user.getOnboardingCompleted())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
