package com.schemebridge.service;

import com.schemebridge.dto.OtpResponse;
import com.schemebridge.dto.SendOtpRequest;
import com.schemebridge.dto.UserDto;
import com.schemebridge.dto.VerifyOtpRequest;

public interface OtpService {
    OtpResponse sendEmailOtp(SendOtpRequest request);
    OtpResponse sendPhoneOtp(SendOtpRequest request);
    UserDto verifyOtp(VerifyOtpRequest request);
}
