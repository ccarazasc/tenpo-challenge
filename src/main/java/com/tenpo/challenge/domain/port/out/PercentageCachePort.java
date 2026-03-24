package com.tenpo.challenge.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Output port: caching contract for the percentage value obtained from the external service.
 */
public interface PercentageCachePort {
    Mono<Double> getPercentage();
    Mono<Void> savePercentage(Double percentage);
}
