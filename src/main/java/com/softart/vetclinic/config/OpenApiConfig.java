package com.softart.vetclinic.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetClinicOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VetClinic API")
                        .description("Multi-tenant veterinary clinic management system REST API. " +
                                "All endpoints (except auth and health) require JWT Bearer token and X-Clinic-Id header.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SoftArt")
                                .email("info@softart.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token from /api/auth/login"))
                        .addParameters("X-Clinic-Id", new Parameter()
                                .in("header")
                                .name("X-Clinic-Id")
                                .description("Tenant clinic UUID")
                                .required(true)
                                .schema(new StringSchema().format("uuid"))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Customizes operations: removes security for auth/health endpoints,
     * and replaces Pageable parameters with clean page/size/sort parameters.
     */
    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            // Tag auth and health endpoints as unsecured
            if (handlerMethod.getBeanType().getSimpleName().equals("AuthController") ||
                    handlerMethod.getBeanType().getSimpleName().equals("HealthController")) {
                operation.setSecurity(List.of());
            }

            // Replace auto-generated Pageable parameters with user-friendly ones
            var params = handlerMethod.getMethodParameters();
            boolean hasPageable = false;
            for (var param : params) {
                if (Pageable.class.isAssignableFrom(param.getParameterType())) {
                    hasPageable = true;
                    break;
                }
            }

            if (hasPageable && operation.getParameters() != null) {
                // Remove auto-generated pageable params
                operation.getParameters().removeIf(p ->
                        "page".equals(p.getName()) ||
                        "size".equals(p.getName()) ||
                        "sort".equals(p.getName()));

                // Add clean parameters with sensible defaults
                operation.addParametersItem(new Parameter()
                        .in("query")
                        .name("page")
                        .description("Page number (0-based)")
                        .required(false)
                        .schema(new IntegerSchema()._default(0).minimum(java.math.BigDecimal.ZERO)));

                operation.addParametersItem(new Parameter()
                        .in("query")
                        .name("size")
                        .description("Page size")
                        .required(false)
                        .schema(new IntegerSchema()._default(20).minimum(java.math.BigDecimal.ONE).maximum(java.math.BigDecimal.valueOf(100))));

                operation.addParametersItem(new Parameter()
                        .in("query")
                        .name("sort")
                        .description("Sort: field,direction (e.g. createdAt,desc)")
                        .required(false)
                        .schema(new StringSchema()._default("createdAt,desc")));
            }

            return operation;
        };
    }
}
