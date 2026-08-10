package com.schemebridge.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SchemeBridge API Gateway Application.
 * <p>
 * Centralized Dynamic Reactive Reverse Proxy and Edge Security Shield.
 * Built on Spring Cloud Gateway (WebFlux / Netty) with dynamic Eureka service discovery routing across microservices.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
