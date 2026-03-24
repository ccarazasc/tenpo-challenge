package com.tenpo.challenge.infrastructure.adapter.out.external;

import com.tenpo.challenge.infrastructure.exception.ExternalServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

@DisplayName("PercentageServiceAdapter")
class PercentageServiceAdapterTest {

    private MockWebServer mockWebServer;
    private PercentageServiceAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        adapter = new PercentageServiceAdapter(webClient);
        // Inject the endpoint via reflection (field is @Value-injected)
        injectField(adapter, "percentageEndpoint", "/api/v1/mock/percentage");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("returns percentage on successful 200 response")
    void getPercentage_success() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"percentage\": 10.0}"));

        StepVerifier.create(adapter.getPercentage())
                .expectNext(10.0)
                .verifyComplete();
    }

    @Test
    @DisplayName("retries up to 3 times on 503, then throws ExternalServiceException")
    void getPercentage_retriesAndFails() {
        // Three consecutive 503s → all retries exhausted
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(503)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"error\": \"service unavailable\"}"));
        }

        StepVerifier.create(adapter.getPercentage())
                .expectError(ExternalServiceException.class)
                .verify();
    }

    @Test
    @DisplayName("succeeds on second attempt after one 503")
    void getPercentage_succeedsOnRetry() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"transient error\"}"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"percentage\": 15.0}"));

        StepVerifier.create(adapter.getPercentage())
                .expectNext(15.0)
                .verifyComplete();
    }

    @Test
    @DisplayName("throws ExternalServiceException on 4xx (not retried)")
    void getPercentage_clientError_notRetried() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"bad request\"}"));

        StepVerifier.create(adapter.getPercentage())
                .expectError(ExternalServiceException.class)
                .verify();
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject field: " + fieldName, e);
        }
    }
}
