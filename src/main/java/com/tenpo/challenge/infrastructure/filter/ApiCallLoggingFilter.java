package com.tenpo.challenge.infrastructure.filter;

import com.tenpo.challenge.domain.model.ApiCall;
import com.tenpo.challenge.domain.port.in.ApiCallHistoryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Asynchronous API call logger.
 *
 * Intercepts every non-excluded request, captures the response body, then
 * fire-and-forget saves the record on a bounded-elastic thread so that logging
 * never blocks the main reactive pipeline.
 *
 * If the save operation itself fails, the error is swallowed and only logged —
 * it must never affect the caller's response (per requirements).
 *
 * Excluded paths: history endpoint (to prevent infinite recursion), mock service,
 * Swagger UI, actuator endpoints.
 *
 * Order(-50) places this after the rate limiter but before the router.
 */
@Slf4j
@Component
@Order(-50)
@RequiredArgsConstructor
public class ApiCallLoggingFilter implements WebFilter {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/api/v1/history",
            "/api/v1/mock",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    );

    private final ApiCallHistoryUseCase historyUseCase;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String parameters = buildParametersString(exchange);
        AtomicReference<String> capturedBody = new AtomicReference<>("");

        BodyCapturingResponse capturingResponse = new BodyCapturingResponse(
                exchange.getResponse(), capturedBody);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .response(capturingResponse)
                .build();

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    boolean success = !capturingResponse.getStatusCode().isError();
                    String responseBody = capturedBody.get();
                    ApiCall apiCall = ApiCall.of(path, parameters, responseBody, success);
                    persistAsync(apiCall);
                });
    }

    private void persistAsync(ApiCall apiCall) {
        historyUseCase.record(apiCall)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        error -> log.error("Failed to persist API call history — record will be lost: {}",
                                error.getMessage())
                );
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String buildParametersString(ServerWebExchange exchange) {
        var queryParams = exchange.getRequest().getQueryParams();
        return queryParams.isEmpty() ? "" : queryParams.toString();
    }

    /**
     * Decorates the response to capture the written bytes before forwarding them downstream.
     * Joins all DataBuffer chunks (important for chunked/streaming responses) into one
     * snapshot, then re-wraps the bytes so the actual response is still written unchanged.
     */
    private static class BodyCapturingResponse extends ServerHttpResponseDecorator {

        private final AtomicReference<String> capturedBody;

        BodyCapturingResponse(ServerHttpResponse delegate, AtomicReference<String> capturedBody) {
            super(delegate);
            this.capturedBody = capturedBody;
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            return DataBufferUtils.join(Flux.from(body))
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        capturedBody.set(new String(bytes, StandardCharsets.UTF_8));
                        return super.writeWith(
                                Mono.just(getDelegate().bufferFactory().wrap(bytes)));
                    });
        }
    }
}
