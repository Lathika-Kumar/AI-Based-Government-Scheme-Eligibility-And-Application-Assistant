package com.schemebridge.authservice.dto.response;

import com.schemebridge.authservice.enums.AccountStatus;
import java.util.Set;

public class UserSummaryResponse {
    private String userId;
    private String email;
    private AccountStatus status;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Set<String> roles;
    private Set<String> permissions;

    public UserSummaryResponse() {}

    public UserSummaryResponse(String userId, String email, AccountStatus status, boolean emailVerified, boolean phoneVerified, Set<String> roles, Set<String> permissions) {
        this.userId = userId;
        this.email = email;
        this.status = status;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.roles = roles;
        this.permissions = permissions;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}
