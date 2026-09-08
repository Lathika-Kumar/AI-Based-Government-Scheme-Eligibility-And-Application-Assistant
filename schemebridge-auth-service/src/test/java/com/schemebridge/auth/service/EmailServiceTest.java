package com.schemebridge.auth.service;

import com.schemebridge.auth.exception.EmailDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService — SmtpEmailService Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smtpEmailService, "mailFrom", "noreply@schemebridge.gov.in");
        ReflectionTestUtils.setField(smtpEmailService, "mailFromName", "SchemeBridge Portal");
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — constructs MimeMessage and calls mailSender.send")
    void testSendEmailVerificationOtp_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                smtpEmailService.sendEmailVerificationOtp("citizen@example.com", "Aarav Sharma", "123456")
        );

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertNotNull(messageCaptor.getValue());
    }

    @Test
    @DisplayName("sendPasswordResetOtp — constructs MimeMessage and calls mailSender.send")
    void testSendPasswordResetOtp_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                smtpEmailService.sendPasswordResetOtp("citizen@example.com", "Aarav Sharma", "654321")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — in production (logRawOtp=false), throws EmailDeliveryException on MailException")
    void testSendEmailVerificationOtp_ProductionFailure() {
        ReflectionTestUtils.setField(smtpEmailService, "logRawOtp", false);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                smtpEmailService.sendEmailVerificationOtp("citizen@example.com", "Aarav Sharma", "123456")
        );

        assertTrue(ex.getMessage().contains("Failed to send OTP email"));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — in dev mode (logRawOtp=true), catches failure without throwing")
    void testSendEmailVerificationOtp_DevModeFallback() {
        ReflectionTestUtils.setField(smtpEmailService, "logRawOtp", true);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                smtpEmailService.sendEmailVerificationOtp("citizen@example.com", "Aarav Sharma", "123456")
        );
    }

}
