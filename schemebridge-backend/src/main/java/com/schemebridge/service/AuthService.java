package com.schemebridge.service;

import com.schemebridge.dto.AuthResponse;
import com.schemebridge.dto.ForgotPasswordRequest;
import com.schemebridge.dto.LoginRequest;
import com.schemebridge.dto.RefreshTokenRequest;
import com.schemebridge.dto.RefreshTokenResponse;
import com.schemebridge.dto.RegisterRequest;
import com.schemebridge.dto.ResetPasswordRequest;
import com.schemebridge.dto.UserDto;

public interface AuthService {
    UserDto registerUser(RegisterRequest registerRequest);
    AuthResponse loginUser(LoginRequest loginRequest);
    RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    void logoutUser();
    String initiateForgotPassword(ForgotPasswordRequest request);
    String resetPassword(ResetPasswordRequest request);
}
