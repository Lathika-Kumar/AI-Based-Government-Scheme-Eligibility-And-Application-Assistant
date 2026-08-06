package com.schemebridge.service.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailProviderConfig {

    @Bean
    @ConditionalOnMissingBean(EmailProvider.class)
    public EmailProvider simulatedEmailProvider() {
        return new SimulatedEmailProvider();
    }
}
