package com.schemebridge.auth.service;

import com.schemebridge.auth.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrevoEmailService — Transactional Email Unit Tests")
class BrevoEmailServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private BrevoEmailService brevoEmailService;

    @BeforeEach
    void setUp() {
        brevoEmailService = new BrevoEmailService(restClient);
        ReflectionTestUtils.setField(brevoEmailService, "brevoEnabled", true);
        ReflectionTestUtils.setField(brevoEmailService, "apiKey", "test-brevo-api-key-12345");
        ReflectionTestUtils.setField(brevoEmailService, "senderEmail", "noreply@schemebridge.gov.in");
        ReflectionTestUtils.setField(brevoEmailService, "senderName", "SchemeBridge");
        ReflectionTestUtils.setField(brevoEmailService, "endpoint", "https://api.brevo.com/v3/smtp/email");
        ReflectionTestUtils.setField(brevoEmailService, "logRawOtp", false);
    }

    @SuppressWarnings("unchecked")
    private void mockRestClientChain(ResponseEntity<BrevoEmailService.BrevoEmailResponse> responseEntity) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(String.class), any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(BrevoEmailService.BrevoEmailResponse.class))).thenReturn(responseEntity);
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — constructs Brevo request and dispatches successfully")
    void testSendEmailVerificationOtp_Success() {
        BrevoEmailService.BrevoEmailResponse mockResponse = new BrevoEmailService.BrevoEmailResponse("<msg-id-12345>");
        mockRestClientChain(new ResponseEntity<>(mockResponse, HttpStatus.CREATED));

        assertDoesNotThrow(() ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Aarav Sharma", "789123")
        );

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(bodyCaptor.capture());

        assertTrue(bodyCaptor.getValue() instanceof BrevoEmailService.BrevoEmailRequest);
        BrevoEmailService.BrevoEmailRequest payload = (BrevoEmailService.BrevoEmailRequest) bodyCaptor.getValue();

        assertEquals("noreply@schemebridge.gov.in", payload.getSender().getEmail());
        assertEquals("SchemeBridge", payload.getSender().getName());
        assertEquals(1, payload.getTo().size());
        assertEquals("citizen@example.com", payload.getTo().get(0).getEmail());
        assertEquals("Aarav Sharma", payload.getTo().get(0).getName());
        assertEquals("SchemeBridge — Email Verification OTP", payload.getSubject());
        assertTrue(payload.getHtmlContent().contains("789123"));
        assertTrue(payload.getTextContent().contains("789123"));
    }

    @Test
    @DisplayName("sendPasswordResetOtp — constructs Brevo request and dispatches successfully")
    void testSendPasswordResetOtp_Success() {
        BrevoEmailService.BrevoEmailResponse mockResponse = new BrevoEmailService.BrevoEmailResponse("<reset-msg-id-67890>");
        mockRestClientChain(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        assertDoesNotThrow(() ->
                brevoEmailService.sendPasswordResetOtp("citizen@example.com", "Sunita Verma", "456789")
        );

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(requestBodySpec).body(bodyCaptor.capture());

        BrevoEmailService.BrevoEmailRequest payload = (BrevoEmailService.BrevoEmailRequest) bodyCaptor.getValue();
        assertEquals("SchemeBridge — Password Reset OTP", payload.getSubject());
        assertTrue(payload.getHtmlContent().contains("456789"));
        assertTrue(payload.getTextContent().contains("456789"));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — throws EmailDeliveryException when BREVO_API_KEY is missing")
    void testSendEmailVerificationOtp_MissingApiKey() {
        ReflectionTestUtils.setField(brevoEmailService, "apiKey", "");

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        assertTrue(ex.getMessage().contains("BREVO_API_KEY"));
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — throws EmailDeliveryException when BREVO_SENDER_EMAIL is missing")
    void testSendEmailVerificationOtp_MissingSenderEmail() {
        ReflectionTestUtils.setField(brevoEmailService, "senderEmail", "");

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        assertTrue(ex.getMessage().contains("BREVO_SENDER_EMAIL"));
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — handles HTTP 400 Bad Request from Brevo")
    void testSendEmailVerificationOtp_Http400() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(String.class), any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientResponseException(
                "Invalid parameters", HttpStatus.BAD_REQUEST.value(), "Bad Request",
                HttpHeaders.EMPTY, "{\"message\":\"invalid email\"}".getBytes(), null));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                brevoEmailService.sendEmailVerificationOtp("invalid-email", "Citizen", "123456")
        );

        assertTrue(ex.getMessage().contains("HTTP 400"));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — handles HTTP 401 Unauthorized from Brevo")
    void testSendEmailVerificationOtp_Http401() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(String.class), any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientResponseException(
                "Unauthorized", HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                HttpHeaders.EMPTY, "{\"message\":\"Key not found\"}".getBytes(), null));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        assertTrue(ex.getMessage().contains("HTTP 401"));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — handles HTTP 500 Internal Server Error from Brevo")
    void testSendEmailVerificationOtp_Http500() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(any(String.class), any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new RestClientResponseException(
                "Server Error", HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                HttpHeaders.EMPTY, "{\"message\":\"Internal error\"}".getBytes(), null));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — completes gracefully when brevoEnabled=false")
    void testSendEmailVerificationOtp_BrevoDisabled() {
        ReflectionTestUtils.setField(brevoEmailService, "brevoEnabled", false);
        ReflectionTestUtils.setField(brevoEmailService, "logRawOtp", false);

        assertDoesNotThrow(() ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("sendEmailVerificationOtp — in dev mode (brevoEnabled=false, logRawOtp=true) logs OTP without network call")
    void testSendEmailVerificationOtp_DevMode() {
        ReflectionTestUtils.setField(brevoEmailService, "brevoEnabled", false);
        ReflectionTestUtils.setField(brevoEmailService, "logRawOtp", true);

        assertDoesNotThrow(() ->
                brevoEmailService.sendEmailVerificationOtp("citizen@example.com", "Citizen", "123456")
        );

        verifyNoInteractions(restClient);
    }
}
