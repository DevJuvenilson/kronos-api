package com.kronos.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Centralizes OpenAPI metadata for the Kronos REST API.
 *
 * <p>Keeping the API description in one configuration class makes the generated
 * documentation consistent across controllers and gives consumers a single,
 * professional entry point for exploring authentication and resource contracts.</p>
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    /**
     * Builds the OpenAPI document exposed by springdoc at {@code /v3/api-docs}.
     *
     * <p>The bearer security scheme enables Swagger UI's Authorize button so
     * authenticated endpoints can be exercised with the same JWT format used by
     * production clients.</p>
     *
     * @return the global OpenAPI definition for the application
     */
    @Bean
    public OpenAPI kronosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kronos API")
                        .version("1.0.0")
                        .description("REST API for managing users, groups, memberships, and collaborative tasks."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a valid JWT token obtained from the authentication endpoints.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
