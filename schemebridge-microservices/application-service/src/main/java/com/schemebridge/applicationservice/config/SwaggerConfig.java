package com.schemebridge.applicationservice.config;

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
    public OpenAPI applicationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemeBridge – Application Service API")
                        .description("""
                                Government Scheme Application Lifecycle Microservice.
                                
                                **Responsibilities:**
                                - Create and manage scheme applications
                                - Application lifecycle: DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED
                                - Duplicate application detection per citizen+scheme
                                - Immutable eligibility snapshot at application time
                                - Complete tracking history (audit trail)
                                - Application dashboard and statistics
                                - Search across citizen's applications
                                - Withdraw active applications
                                
                                **Statuses:** DRAFT | SUBMITTED | UNDER_REVIEW | DOCUMENT_PENDING | VERIFIED | APPROVED | REJECTED | WITHDRAWN | BENEFIT_RELEASED
                                
                                **Application Number Format:** SB-APP-YYYY-NNNNNN
                                
                                **Port:** 8084
                                **Database:** MongoDB (schemebridge_application_db)
                                **Future Integrations:** Citizen Service (8082), Scheme Service (8083), Document Service (8085), Notification Service (8086)
                                """)
                        .version("v1 (0.0.1-SNAPSHOT)")
                        .contact(new Contact()
                                .name("SchemeBridge Team")
                                .email("admin@schemebridge.gov.in"))
                        .license(new License().name("Government Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Direct: Application Service"),
                        new Server().url("http://localhost:8080").description("Gateway: API Gateway → Application Service")
                ));
    }
}
