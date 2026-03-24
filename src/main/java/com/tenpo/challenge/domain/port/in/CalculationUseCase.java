package com.tenpo.challenge.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Input port: calculates the sum of two numbers applying a dynamic external percentage.
 */
public interface CalculationUseCase {
    Mono<Double> calculate(double num1, double num2);
}
