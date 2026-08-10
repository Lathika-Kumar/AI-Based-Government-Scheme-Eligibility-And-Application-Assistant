package com.schemebridge.documentservice.config;

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
    public OpenAPI documentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemeBridge – Document Service API")
                        .description("""
                                Citizen Document Upload, Storage, Versioning, Verification & Readiness Microservice.
                                
                                **Key Features:**
                                - Storage abstraction (LocalStorageService -> AWS S3 / Azure Blob / MinIO)
                                - SHA-256 Checksum calculation & Duplicate upload prevention
                                - Version history preservation on re-upload
                                - Verification workflow: UPLOADED -> UNDER_REVIEW -> VERIFIED / REJECTED
                                - File security validation (prohibits dangerous extensions .exe, .bat, .sh)
                                - Audit trail tracking (uploadedBy, verifiedBy, IP, action, timestamp)
                                - Document readiness calculation for Citizen Service
                                
                                **Port:** 8085
                                **Database:** MongoDB (schemebridge_document_db)
                                **Storage:** /uploads/documents (local)
                                """)
                        .version("v1 (0.0.1-SNAPSHOT)")
                        .contact(new Contact()
                                .name("SchemeBridge Team")
                                .email("admin@schemebridge.gov.in"))
                        .license(new License().name("Government Use Only")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Direct: Document Service"),
                        new Server().url("http://localhost:8080").description("Gateway: API Gateway -> Document Service")
                ));
    }
}
