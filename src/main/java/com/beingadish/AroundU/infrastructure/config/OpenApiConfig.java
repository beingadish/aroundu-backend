package com.beingadish.AroundU.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile({"dev", "test", "preprod"})
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Enter your JWT token obtained from POST /api/v1/auth/login",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI aroundUOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AroundU API")
                        .description("""
                                AroundU is a local services marketplace connecting clients with skilled workers.
                                
                                ## Authentication
                                Most endpoints require a JWT Bearer token. Obtain one via `POST /api/v1/auth/login`.
                                
                                ## Rate Limiting
                                Certain endpoints enforce rate limits. Exceeding the limit returns `429 Too Many Requests`.
                                
                                ## Pagination
                                List endpoints accept `page` (0-based) and `size` (max 100) query parameters.
                                Responses include `totalElements`, `totalPages`, and `last` metadata.
                                
                                ## Sorting
                                Sortable endpoints accept `sortBy`, `sortDirection`, `secondarySortBy`, and `secondarySortDirection`.
                                Distance-based sorting is available via `sortByDistance=true` with latitude/longitude coordinates.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AroundU Dev Team")
                                .email("support@aroundu.com")
                                .url("https://aroundu.com"))
                        .license(new License().name("Proprietary"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:20232").description("Local development"),
                        new Server().url("https://preprod.aroundu.com").description("Pre-production")
                ));
    }
}
