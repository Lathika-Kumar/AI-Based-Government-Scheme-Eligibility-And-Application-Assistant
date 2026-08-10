package com.schemebridge.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(StartupConfigLogger.class)
public class SwaggerAutoConfiguration {
}
