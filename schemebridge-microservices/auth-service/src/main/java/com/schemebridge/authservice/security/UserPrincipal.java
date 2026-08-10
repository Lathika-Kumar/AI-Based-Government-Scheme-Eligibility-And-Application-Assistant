package com.schemebridge.authservice.security;

import com.schemebridge.authservice.entity.PermissionEntity;
import com.schemebridge.authservice.entity.RoleEntity;
import com.schemebridge.authservice.entity.UserEntity;
import com.schemebridge.authservice.enums.AccountStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String email;
    private final String passwordHash;
    private final AccountStatus status;
    private final boolean accountLocked;
    private final Set<GrantedAuthority> authorities;

    public UserPrincipal(String userId, String email, String passwordHash, AccountStatus status, boolean accountLocked, Set<GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.accountLocked = accountLocked;
        this.authorities = authorities;
    }

    public static UserPrincipal create(UserEntity user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (RoleEntity role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));
            for (PermissionEntity permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()));
            }
        }
        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus(),
                user.isAccountLocked(),
                authorities
        );
    }

    public String getUserId() { return userId; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked && status != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE || status == AccountStatus.PENDING_VERIFICATION;
    }
}
