package com.schemebridge.schemeservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI schemeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemeBridge – Scheme Service API")
                        .description("""
                                Government Scheme Catalog, Eligibility Rules & Recommendation Microservice.
                                
                                **Responsibilities:**
                                - Scheme catalog management (CRUD)
                                - Dynamic eligibility rule evaluation
                                - Featured & newly added schemes
                                - Trending schemes
                                - Recommendation support for Citizen Service
                                - Multilingual metadata (English + Tamil)
                                - Scheme versioning & state-specific availability
                                
                                **Port:** 8083
                                **Database:** Oracle XE (XEPDB1)
                                **Called by:** Citizen Service (EligibilityServiceClient)
                                """)
                        .version("v1 (0.0.1-SNAPSHOT)")
                        .contact(new Contact()
                                .name("SchemeBridge Team")
                                .email("admin@schemebridge.gov.in"))
                        .license(new License().name("Government Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Direct: Scheme Service"),
                        new Server().url("http://localhost:8080").description("Gateway: API Gateway → Scheme Service")
                ));
    }
}
