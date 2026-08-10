package com.schemebridge.citizenservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI citizenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchemeBridge Citizen Service API")
                        .description("Microservice for Citizen Profile Management, Demographic Data, Saved Schemes, and Dashboard Summaries with MongoDB")
                        .version("v0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("SchemeBridge Engineering Team")
                                .email("support@schemebridge.gov.in"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
