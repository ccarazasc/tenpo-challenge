package com.tenpo.challenge.infrastructure.adapter.out.external;

import com.tenpo.challenge.domain.port.out.PercentageServicePort;
import com.tenpo.challenge.infrastructure.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Calls the external (mocked) percentage service.
 *
 * Retry strategy: up to 3 total attempts (2 retries) with a 500 ms fixed delay.
 * Only transient errors (5xx, network issues) are retried.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PercentageServiceAdapter implements PercentageServicePort {

    private final WebClient percentageWebClient;

    @Value("${app.percentage-service.endpoint:/api/v1/mock/percentage}")
    private String percentageEndpoint;

    @Override
    public Mono<Double> getPercentage() {
        return percentageWebClient.get()
                .uri(percentageEndpoint)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new ExternalServiceException(
                                        "Percentage service responded with %d: %s"
                                                .formatted(response.statusCode().value(), body))))
                )
                .bodyToMono(PercentageResponse.class)
                .map(PercentageResponse::percentage)
                .retryWhen(
                        Retry.fixedDelay(2, Duration.ofMillis(500))
                                .filter(this::isRetryable)
                                .doBeforeRetry(signal -> log.warn(
                                        "Retrying percentage service call — attempt {}/3: {}",
                                        signal.totalRetries() + 2, signal.failure().getMessage()))
                                .onRetryExhaustedThrow((spec, signal) ->
                                        new ExternalServiceException(
                                                "Percentage service failed after 3 attempts",
                                                signal.failure()))
                );
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ExternalServiceException ex) {
            return ex.getMessage() != null && ex.getMessage().contains("5");
        }
        return throwable instanceof WebClientResponseException.ServiceUnavailable
                || throwable instanceof java.io.IOException;
    }

    private record PercentageResponse(double percentage) {}
}
