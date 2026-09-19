package com.schemebridge.auth.service;

import com.schemebridge.auth.dto.request.SignupRequest;
import com.schemebridge.auth.repository.OtpVerificationRepository;
import com.schemebridge.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmailService emailService;

    @SpyBean
    private OtpVerificationRepository otpVerificationRepository;



    @Test
    public void testSignup_RollbackOnFailure() {
        // Arrange: Spy on the OTP repository to throw an exception when saving OTP
        doThrow(new RuntimeException("Simulated database failure during OTP saving"))
                .when(otpVerificationRepository).save(any());

        String email = "rollback_" + System.currentTimeMillis() + "@example.com";
        SignupRequest request = SignupRequest.builder()
                .firstName("Rollback")
                .lastName("User")
                .email(email)
                .password("StrongPassword123!")
                .phoneNumber("9876543210")
                .dob(java.time.LocalDate.of(2000, 1, 1))
                .build();

        // Act & Assert: Signup should fail because of simulated OTP saving exception
        assertThrows(RuntimeException.class, () -> authService.signup(request));

        // Assert: The user table insert should have rolled back due to @Transactional
        boolean userExists = userRepository.existsByEmail(email);
        assertFalse(userExists, "The user insertion should have been rolled back and not persisted in database");
    }
}
