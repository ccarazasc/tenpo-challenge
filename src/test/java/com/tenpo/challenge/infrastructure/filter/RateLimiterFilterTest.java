package com.tenpo.challenge.infrastructure.filter;

import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.infrastructure.adapter.in.web.CalculationController;
import com.tenpo.challenge.infrastructure.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CalculationController.class)
@Import({RateLimiterFilter.class, GlobalExceptionHandler.class})
@DisplayName("RateLimiterFilter")
class RateLimiterFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CalculationUseCase calculationUseCase;

    @MockBean
    private ApiCallHistoryUseCase apiCallHistoryUseCase;

    @MockBean
    private ReactiveStringRedisTemplate redisTemplate;

    @MockBean
    private ReactiveValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(calculationUseCase.calculate(anyDouble(), anyDouble())).thenReturn(Mono.just(11.0));
    }

    @Test
    @DisplayName("allows requests within the rate limit")
    void allowsRequestsWithinLimit() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));

        webTestClient.get()
                .uri("/api/v1/calculate?num1=5&num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("blocks requests exceeding the rate limit with 429")
    void blocksRequestsExceedingLimit() {
        // Simulate counter already at 4 (> 3 RPM limit)
        when(valueOps.increment(anyString())).thenReturn(Mono.just(4L));

        webTestClient.get()
                .uri("/api/v1/calculate?num1=5&num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.status").isEqualTo(429);
    }

    @Test
    @DisplayName("third request is still allowed (boundary)")
    void thirdRequestIsAllowed() {
        when(valueOps.increment(anyString())).thenReturn(Mono.just(3L));

        webTestClient.get()
                .uri("/api/v1/calculate?num1=5&num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}
