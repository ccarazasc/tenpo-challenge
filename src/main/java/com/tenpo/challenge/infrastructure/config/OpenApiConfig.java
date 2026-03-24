package com.tenpo.challenge.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI tenpoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenpo Challenge API")
                        .version("1.0.0")
                        .description("""
                                REST API built with **Spring Boot 3 + WebFlux** (reactive) for the Tenpo Technical Lead challenge.

                                ### Key features
                                - `GET /api/v1/calculate` — sums two numbers and applies a dynamic external percentage
                                - `GET /api/v1/history` — paginated log of all API calls
                                - Distributed Redis cache for the percentage value (30-min TTL)
                                - Retry logic: up to 3 attempts on external-service failures
                                - Redis-backed rate limiter: 3 RPM (configurable)
                                - Async call logging: writes never block the main response
                                """)
                        .contact(new Contact()
                                .name("Tenpo Engineering")
                                .email("engineering@tenpo.cl")
                                .url("https://tenpo.cl"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)
                                .description("Local / Docker server")
                ));
    }
}
