package com.tenpo.challenge.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates an external percentage service.
 *
 * Configurable failure behaviour via application properties:
 *  - app.mock.failure-enabled  → activates failure simulation
 *  - app.mock.fail-every-n-calls → fails every N-th call (default: every 2nd call when enabled)
 *
 * This lets integration tests exercise the retry + cache fallback paths.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mock")
@Tag(name = "Mock", description = "Simulated external percentage provider")
public class MockPercentageController {

    @Value("${app.mock.percentage:10.0}")
    private double percentage;

    @Value("${app.mock.failure-enabled:false}")
    private boolean failureEnabled;

    @Value("${app.mock.fail-every-n-calls:2}")
    private int failEveryNCalls;

    private final AtomicInteger callCounter = new AtomicInteger(0);

    @GetMapping("/percentage")
    @Operation(summary = "Returns a fixed percentage (simulates an external service)")
    public Mono<PercentageResponse> getPercentage() {
        int count = callCounter.incrementAndGet();
        log.debug("Mock percentage service called (call #{})", count);

        if (failureEnabled && count % failEveryNCalls == 0) {
            log.warn("Mock service simulating failure on call #{}", count);
            return Mono.error(new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Simulated external service failure"));
        }

        return Mono.just(new PercentageResponse(percentage));
    }

    record PercentageResponse(double percentage) {}
}
