package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.infrastructure.exception.PercentageServiceUnavailableException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(CalculationController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("CalculationController")
class CalculationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CalculationUseCase calculationUseCase;

    @MockBean
    private ApiCallHistoryUseCase apiCallHistoryUseCase;

    @MockBean
    private ReactiveStringRedisTemplate redisTemplate;

    @MockBean
    @SuppressWarnings("unchecked")
    private ReactiveValueOperations<String, String> valueOps;

    @BeforeEach
    void setUpFilters() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(apiCallHistoryUseCase.record(any(ApiCall.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("GET /api/v1/calculate returns 200 with correct result")
    void calculate_success() {
        when(calculationUseCase.calculate(5.0, 5.0)).thenReturn(Mono.just(11.0));

        webTestClient.get()
                .uri("/api/v1/calculate?num1=5&num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.num1").isEqualTo(5.0)
                .jsonPath("$.num2").isEqualTo(5.0)
                .jsonPath("$.result").isEqualTo(11.0);
    }

    @Test
    @DisplayName("GET /api/v1/calculate returns 503 when service is unavailable and cache is empty")
    void calculate_serviceUnavailable() {
        when(calculationUseCase.calculate(anyDouble(), anyDouble()))
                .thenReturn(Mono.error(new PercentageServiceUnavailableException(
                        "Percentage service is unavailable and no cached value exists")));

        webTestClient.get()
                .uri("/api/v1/calculate?num1=5&num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/calculate returns 400 when num1 is missing")
    void calculate_missingParam() {
        webTestClient.get()
                .uri("/api/v1/calculate?num2=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
