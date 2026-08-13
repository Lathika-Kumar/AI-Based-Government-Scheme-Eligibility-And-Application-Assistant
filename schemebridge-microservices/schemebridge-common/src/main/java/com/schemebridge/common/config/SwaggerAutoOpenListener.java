package com.schemebridge.common.config;

import lombok.extern.slf4j.Slf4j;

/**
 * Legacy/Duplicate Swagger Auto Open Listener.
 * <p>
 * DISABLED to prevent duplicate browser tab launches on startup.
 * The primary, single auto-launcher is handled by StartupConfigLogger.
 */
@Slf4j
public class SwaggerAutoOpenListener {
    // Disabled — StartupConfigLogger is the single source of truth for Swagger auto-launching.
}
