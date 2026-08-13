package com.schemebridge.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single, authoritative OS cross-process launcher for Swagger UI.
 * <p>
 * Uses OS-level file channel locks to guarantee that EXACTLY ONE browser tab
 * opens per microservice port, even across multiple forked Java processes (e.g. Maven spring-boot:run forks).
 */
public class StartupConfigLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger("SwaggerAutoLauncher");
    private static final AtomicBoolean JVM_LAUNCHED = new AtomicBoolean(false);
    private static final ConcurrentHashMap<String, FileChannel> HELD_LOCKS = new ConcurrentHashMap<>();

    private final Environment environment;

    public StartupConfigLogger(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String appName = environment.getProperty("spring.application.name", "Unknown-Service");
        String port = environment.getProperty("server.port", "8080");

        // Skip Swagger auto-launch for infrastructure services (config-server, service-registry, api-gateway)
        if (appName.equalsIgnoreCase("config-server") ||
            appName.equalsIgnoreCase("service-registry") ||
            appName.equalsIgnoreCase("api-gateway")) {
            log.info("SwaggerAutoLauncher: Skipping Swagger auto-launch for infrastructure service '{}'", appName);
            return;
        }

        Boolean autoOpenSwagger = environment.getProperty("schemebridge.auto-open-swagger", Boolean.class, true);
        if (Boolean.FALSE.equals(autoOpenSwagger)) {
            log.info("SwaggerAutoLauncher: Auto-open Swagger is disabled via configuration");
            return;
        }

        // 1. In-JVM Guard Check
        if (!JVM_LAUNCHED.compareAndSet(false, true)) {
            log.info("SWAGGER_LAUNCH_BLOCKED = In-JVM guard blocked duplicate invocation for {}", appName);
            return;
        }

        // 2. OS Cross-Process Lock Check (Prevents dual launches from Maven/Spring Boot forked JVM processes)
        if (!acquireCrossProcessLock(port)) {
            log.info("SWAGGER_LAUNCH_BLOCKED = OS cross-process lock already held by another Java process for port {}", port);
            return;
        }

        String uiPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui/index.html");
        if (!uiPath.startsWith("/")) {
            uiPath = "/" + uiPath;
        }

        String swaggerUrl = "http://localhost:" + port + uiPath;

        // Diagnostic Telemetry Logging
        log.info("SWAGGER_LAUNCH_TRIGGER = {}", this.getClass().getName());
        log.info("SWAGGER_LAUNCH_CLASS = {}", this.getClass().getCanonicalName());
        log.info("SWAGGER_LAUNCH_THREAD = {}", Thread.currentThread().getName());
        log.info("SWAGGER_LAUNCH_URL = {}", swaggerUrl);

        CompletableFuture.runAsync(() -> openBrowser(swaggerUrl));
    }

    private boolean acquireCrossProcessLock(String port) {
        try {
            String lockFilePath = System.getProperty("java.io.tmpdir") + File.separator + "schemebridge_swagger_" + port + ".lock";
            FileChannel channel = FileChannel.open(Paths.get(lockFilePath),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.READ);
            FileLock lock = channel.tryLock();
            if (lock != null) {
                HELD_LOCKS.put(port, channel);
                return true;
            }
        } catch (Exception e) {
            log.warn("SwaggerAutoLauncher: OS lock acquisition check notice: {}", e.getMessage());
        }
        return false;
    }

    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                log.info("SWAGGER_LAUNCH_METHOD = ProcessBuilder(cmd /c start \"\" \"{}\")", url);
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
                return;
            } else if (os.contains("mac")) {
                log.info("SWAGGER_LAUNCH_METHOD = ProcessBuilder(open \"{}\")", url);
                new ProcessBuilder("open", url).start();
                return;
            } else if (os.contains("nix") || os.contains("nux")) {
                log.info("SWAGGER_LAUNCH_METHOD = ProcessBuilder(xdg-open \"{}\")", url);
                new ProcessBuilder("xdg-open", url).start();
                return;
            }

            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                log.info("SWAGGER_LAUNCH_METHOD = Desktop.browse({})", url);
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            log.warn("SwaggerAutoLauncher: Failed to auto-open browser: {}", e.getMessage());
        }
    }
}
