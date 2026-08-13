package com.schemebridge.common.config;

import com.schemebridge.common.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for common SchemeBridge infrastructure beans.
 * Imports StartupConfigLogger for single, safe Swagger UI browser launching on ApplicationReadyEvent.
 */
@AutoConfiguration
@Import({StartupConfigLogger.class, DomainEventPublisher.class})
public class SwaggerAutoConfiguration {
}
