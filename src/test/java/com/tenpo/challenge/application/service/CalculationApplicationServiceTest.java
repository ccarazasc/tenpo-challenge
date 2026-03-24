package com.tenpo.challenge.application.service;

import com.tenpo.challenge.domain.port.out.PercentageCachePort;
import com.tenpo.challenge.domain.port.out.PercentageServicePort;
import com.tenpo.challenge.infrastructure.exception.PercentageServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculationApplicationService")
class CalculationApplicationServiceTest {

    @Mock
    private PercentageServicePort percentageServicePort;

    @Mock
    private PercentageCachePort percentageCachePort;

    @InjectMocks
    private CalculationApplicationService service;

    @BeforeEach
    void setUp() {
        when(percentageCachePort.savePercentage(anyDouble())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("returns (num1+num2) + percentage% when external service is healthy")
    void calculate_happyPath() {
        when(percentageServicePort.getPercentage()).thenReturn(Mono.just(10.0));

        StepVerifier.create(service.calculate(5.0, 5.0))
                .expectNext(11.0)
                .verifyComplete();

        verify(percentageCachePort).savePercentage(10.0);
    }

    @Test
    @DisplayName("falls back to cached percentage when external service fails")
    void calculate_fallbackToCache() {
        when(percentageServicePort.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("service down")));
        when(percentageCachePort.getPercentage()).thenReturn(Mono.just(20.0));

        StepVerifier.create(service.calculate(10.0, 10.0))
                .expectNext(24.0)   // (10+10) + 20% = 24
                .verifyComplete();
    }

    @Test
    @DisplayName("propagates 503 error when service fails AND cache is empty")
    void calculate_noServiceNoCache() {
        when(percentageServicePort.getPercentage())
                .thenReturn(Mono.error(new RuntimeException("service down")));
        when(percentageCachePort.getPercentage()).thenReturn(Mono.empty());

        StepVerifier.create(service.calculate(5.0, 5.0))
                .expectError(PercentageServiceUnavailableException.class)
                .verify();
    }

    @Test
    @DisplayName("cache write failure does not affect the calculation result")
    void calculate_cacheWriteFailureIsSwallowed() {
        when(percentageServicePort.getPercentage()).thenReturn(Mono.just(10.0));
        when(percentageCachePort.savePercentage(anyDouble()))
                .thenReturn(Mono.error(new RuntimeException("redis down")));

        StepVerifier.create(service.calculate(5.0, 5.0))
                .expectNext(11.0)
                .verifyComplete();
    }

    @Test
    @DisplayName("applies zero percent correctly")
    void calculate_zeroPercentage() {
        when(percentageServicePort.getPercentage()).thenReturn(Mono.just(0.0));

        StepVerifier.create(service.calculate(4.0, 6.0))
                .expectNext(10.0)
                .verifyComplete();
    }

    @Test
    @DisplayName("applies 100% percentage (doubles the sum)")
    void calculate_hundredPercent() {
        when(percentageServicePort.getPercentage()).thenReturn(Mono.just(100.0));

        StepVerifier.create(service.calculate(5.0, 5.0))
                .expectNext(20.0)   // (5+5) + 100% = 20
                .verifyComplete();
    }
}
