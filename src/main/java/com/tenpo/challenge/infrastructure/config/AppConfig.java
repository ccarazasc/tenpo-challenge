package com.tenpo.challenge.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${app.percentage-service.base-url:http://localhost:8080}")
    private String percentageServiceBaseUrl;

    /**
     * WebClient targeting the (mock) percentage service.
     * The base URL is externalised so it can be overridden in Docker / K8s
     * without code changes.
     */
    @Bean
    public WebClient percentageWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(percentageServiceBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
