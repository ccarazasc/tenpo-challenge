package com.tenpo.challenge.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Output port: retrieves the dynamic percentage from an external service.
 */
public interface PercentageServicePort {
    Mono<Double> getPercentage();
}
