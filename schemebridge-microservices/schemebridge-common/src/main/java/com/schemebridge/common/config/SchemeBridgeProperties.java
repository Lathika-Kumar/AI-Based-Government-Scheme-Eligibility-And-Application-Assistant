package com.schemebridge.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "schemebridge")
public class SchemeBridgeProperties {

    private String version = "1.0.0-MODULE12";
    private String environment = "development";
    private Features features = new Features();
    private Security security = new Security();

    @Data
    public static class Features {
        private boolean notifications = true;
        private boolean redis = false;
        private boolean kafka = false;
        private boolean sms = true;
        private boolean email = true;
        private boolean ai = false;
        private boolean audit = true;
        private boolean analytics = true;
    }

    @Data
    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMs = 86400000L;
        private long jwtRefreshExpirationMs = 604800000L;
    }
}
