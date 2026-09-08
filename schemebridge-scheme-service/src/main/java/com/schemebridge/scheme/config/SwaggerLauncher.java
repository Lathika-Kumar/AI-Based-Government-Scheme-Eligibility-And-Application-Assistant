package com.schemebridge.scheme.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class SwaggerLauncher {

    @EventListener(ApplicationReadyEvent.class)
    public void launchSwagger() {
        // Skip opening browser during testing
        if (isTestEnvironment()) {
            log.info("Skipping Swagger auto-launch in test environment.");
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String url = "http://localhost:8081/swagger-ui/index.html";
        log.info("Application is ready. Automatically launching Swagger UI at: {}", url);

        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", url).start();
            } else {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.error("Failed to automatically launch Swagger UI: {}", e.getMessage());
        }
    }

    private boolean isTestEnvironment() {
        // Check if SpringBootTest is active or if class is loaded inside a test runner
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("org.junit.") || 
                element.getClassName().contains("org.springframework.test.") ||
                element.getClassName().contains("Surefire")) {
                return true;
            }
        }
        return false;
    }
}
