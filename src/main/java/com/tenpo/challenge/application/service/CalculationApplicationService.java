package com.tenpo.challenge.application.service;

import com.tenpo.challenge.domain.port.in.CalculationUseCase;
import com.tenpo.challenge.domain.port.out.PercentageCachePort;
import com.tenpo.challenge.domain.port.out.PercentageServicePort;
import com.tenpo.challenge.infrastructure.exception.PercentageServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Core business logic: sums two numbers and applies a dynamic external percentage.
 *
 * Fallback strategy (per requirements):
 *   1. Fetch percentage from external service.
 *   2. On success → update cache, return calculated result.
 *   3. On failure → attempt to use last known cached value.
 *   4. If cache is also empty → propagate error with 503.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationApplicationService implements CalculationUseCase {

    private final PercentageServicePort percentageServicePort;
    private final PercentageCachePort percentageCachePort;

    @Override
    public Mono<Double> calculate(double num1, double num2) {
        double sum = num1 + num2;

        return percentageServicePort.getPercentage()
                .flatMap(percentage ->
                        percentageCachePort.savePercentage(percentage)
                                .onErrorResume(cacheError -> {
                                    log.warn("Failed to persist percentage to cache: {}", cacheError.getMessage());
                                    return Mono.empty();
                                })
                                .thenReturn(percentage)
                )
                .onErrorResume(serviceError -> {
                    log.warn("Percentage service unavailable ({}), falling back to cache", serviceError.getMessage());
                    return percentageCachePort.getPercentage()
                            .switchIfEmpty(Mono.error(new PercentageServiceUnavailableException(
                                    "Percentage service is unavailable and no cached value exists")));
                })
                .map(percentage -> applyPercentage(sum, percentage));
    }

    private double applyPercentage(double value, double percentage) {
        return value + (value * percentage / 100.0);
    }
}
