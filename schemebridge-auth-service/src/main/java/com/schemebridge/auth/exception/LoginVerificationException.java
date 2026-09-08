package com.schemebridge.auth.exception;

public class LoginVerificationException extends RuntimeException {
    public LoginVerificationException(String message) {
        super(message);
    }
}
