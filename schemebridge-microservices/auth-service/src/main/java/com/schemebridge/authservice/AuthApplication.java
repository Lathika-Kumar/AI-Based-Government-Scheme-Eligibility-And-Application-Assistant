package com.schemebridge.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SchemeBridge Authentication & Identity Microservice Main Entry Point.
 * Running on Port 8081 with Spring Boot 3.2.3, Spring Security 6, Oracle DB, and Brevo API.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
