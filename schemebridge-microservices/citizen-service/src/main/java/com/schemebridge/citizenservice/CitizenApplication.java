package com.schemebridge.citizenservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * SchemeBridge Citizen Service Main Application Entry Point.
 * Running on Port 8082 with Spring Boot 3.2.3 and MongoDB.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
public class CitizenApplication {

    public static void main(String[] args) {
        SpringApplication.run(CitizenApplication.class, args);
    }
}
