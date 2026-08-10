package com.schemebridge.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StartupConfigLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger("SwaggerAutoLauncher");

    private final Environment environment;
    private final AtomicBoolean hasLaunched = new AtomicBoolean(false);

    public StartupConfigLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!hasLaunched.compareAndSet(false, true)) {
            return;
        }

        String appName = environment.getProperty("spring.application.name", "Unknown-Service");
        String port = environment.getProperty("server.port", "8080");

        log.info("SwaggerAutoLauncher: Application is ready ({}, Port {})", appName, port);

        if (appName.equalsIgnoreCase("config-server") || appName.equalsIgnoreCase("service-registry") || appName.equalsIgnoreCase("api-gateway")) {
            log.info("SwaggerAutoLauncher: Skipping Swagger auto-launch for infrastructure service '{}'", appName);
            return;
        }

        Boolean autoOpenSwagger = environment.getProperty("schemebridge.auto-open-swagger", Boolean.class, true);
        if (Boolean.FALSE.equals(autoOpenSwagger)) {
            log.info("SwaggerAutoLauncher: Auto-open Swagger is disabled via configuration");
            return;
        }

        String swaggerUrl = "http://localhost:" + port + "/swagger-ui/index.html";
        log.info("SwaggerAutoLauncher: Opening {}", swaggerUrl);

        CompletableFuture.runAsync(() -> openBrowser(swaggerUrl));
    }

    private void openBrowser(String url) {
        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            log.warn("SwaggerAutoLauncher: Failed to auto-open browser: {}", e.getMessage());
        }
    }
}
