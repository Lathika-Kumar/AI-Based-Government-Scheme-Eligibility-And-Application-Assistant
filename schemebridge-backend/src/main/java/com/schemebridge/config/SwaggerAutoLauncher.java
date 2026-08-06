package com.schemebridge.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SwaggerAutoLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!shouldOpenBrowser()) {
            return;
        }

        Thread browserThread = new Thread(() -> {
            try {
                Thread.sleep(1500L);
                String host = environment.getProperty("server.address", "localhost");
                String port = environment.getProperty("server.port", "8080");
                String contextPath = environment.getProperty("server.servlet.context-path", "");
                String normalizedContextPath = (contextPath == null || "/".equals(contextPath)) ? "" : contextPath;
                String swaggerUrl = String.format("http://%s:%s%s/swagger-ui/index.html", host, port, normalizedContextPath);

                String osName = System.getProperty("os.name", "").toLowerCase();
                if (!osName.contains("win")) {
                    log.debug("Non-Windows OS detected ({}). Skipping Swagger auto-open for {}.", osName, swaggerUrl);
                    return;
                }

                Process process = new ProcessBuilder("cmd", "/c", "start", swaggerUrl).start();
                process.waitFor();
                log.info("Opened Swagger UI automatically at {}", swaggerUrl);
            } catch (Exception ex) {
                log.debug("Swagger auto-launch was skipped because the browser could not be opened.", ex);
            }
        }, "swagger-auto-launcher");

        browserThread.setDaemon(true);
        browserThread.start();
    }

    private boolean shouldOpenBrowser() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.stream().anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production") || profile.equalsIgnoreCase("test"))) {
            return false;
        }

        String explicitFlag = environment.getProperty("schemebridge.swagger.auto-open");
        if (explicitFlag != null) {
            return Boolean.parseBoolean(explicitFlag);
        }

        return true;
    }
}
