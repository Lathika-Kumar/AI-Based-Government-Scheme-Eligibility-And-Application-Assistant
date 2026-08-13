package com.schemebridge.coreservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CoreServiceSwaggerConfig {

    @Bean
    public OpenAPI coreServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemeBridge – Core Service API")
                        .description("""
                                Consolidated Government Core Services API (Port 8082).
                                
                                **Unified Domains:**
                                1. **Citizen Domain:** Profile management, demographic data, document readiness, saved schemes.
                                2. **Scheme Domain:** Catalog management, eligibility rules, scheme categories, departments, recommendations.
                                3. **Application Domain:** Scheme application lifecycle (DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED), application tracking timeline.
                                4. **Document Domain:** Citizen document upload, storage, versioning, SHA-256 checksum validation, officer verification workflow.
                                
                                **Port:** 8082
                                **Databases:** MongoDB (applications, citizen profiles, documents) + Oracle XE (schemes, categories, rules)
                                """)
                        .version("v1 (0.0.1-SNAPSHOT)")
                        .contact(new Contact()
                                .name("SchemeBridge Engineering Team")
                                .email("support@schemebridge.gov.in"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Direct: Core Service"),
                        new Server().url("http://localhost:8080").description("Gateway: API Gateway → Core Service")
                ));
    }
}
