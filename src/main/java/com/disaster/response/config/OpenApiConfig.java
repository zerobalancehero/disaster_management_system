package com.disaster.response.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI disasterOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("AI-Based Disaster Prediction & Emergency Response System API")
                        .description("Production-grade backend REST API supporting disaster alerts, weather forecasts, " +
                                "GIS/DEM processing, ML-driven risk inference, and emergency team dispatch.")
                        .version("1.0.0")
                        .contact(new Contact().name("Disaster Management Team").email("emergency-ops@disaster.gov"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT Bearer token obtained from /api/v1/auth/login")));
    }
}
