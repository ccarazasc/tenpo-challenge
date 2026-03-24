package com.tenpo.challenge.infrastructure.adapter.in.web;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.model.Page;
import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import com.tenpo.challenge.infrastructure.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(ApiCallHistoryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ApiCallHistoryController")
class ApiCallHistoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ApiCallHistoryUseCase historyUseCase;

    @Test
    @DisplayName("GET /api/v1/history returns paginated results")
    void getHistory_returnsPaginatedData() {
        var entry = new ApiCall(1L, "/api/v1/calculate", "{num1=[5.0], num2=[5.0]}",
                "{\"result\":11.0}", true, LocalDateTime.now());
        var page = new Page<>(List.of(entry), 0, 10, 1L, 1);

        when(historyUseCase.getHistory(anyInt(), anyInt())).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri("/api/v1/history?page=0&size=10")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content[0].endpoint").isEqualTo("/api/v1/calculate")
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.first").isEqualTo(true)
                .jsonPath("$.last").isEqualTo(true);
    }

    @Test
    @DisplayName("GET /api/v1/history returns empty page when no calls recorded")
    void getHistory_emptyResult() {
        var emptyPage = new Page<ApiCall>(List.of(), 0, 10, 0L, 0);
        when(historyUseCase.getHistory(anyInt(), anyInt())).thenReturn(Mono.just(emptyPage));

        webTestClient.get()
                .uri("/api/v1/history")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.totalElements").isEqualTo(0);
    }
}
