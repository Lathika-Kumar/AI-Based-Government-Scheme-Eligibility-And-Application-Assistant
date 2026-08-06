package com.schemebridge.service;

import com.schemebridge.service.email.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailProvider emailProvider;

    public void sendEmail(String to, String subject, String htmlBody, String textBody) {
        emailProvider.sendEmail(to, subject, htmlBody, textBody);
    }
}
