package com.tenpo.challenge.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Distributed rate limiter implemented via Redis fixed-window counter.
 *
 * Key: tenpo:rate_limit:<epoch_minute>
 *  → incremented atomically, TTL of 60 s.
 *  → works correctly across multiple replicas because Redis is the shared state.
 *
 * Excluded paths (infra / docs) are not rate-limited.
 * Responds with HTTP 429 and a JSON body when the threshold is exceeded.
 *
 * Order(-100) ensures this runs before the logging filter and business logic.
 */
@Slf4j
@Component
@Order(-100)
public class RateLimiterFilter implements WebFilter {

    private static final String KEY_PREFIX = "tenpo:rate_limit:";
    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/webjars"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final int maxRequestsPerMinute;

    public RateLimiterFilter(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.max-requests-per-minute:3}") int maxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isExcluded(path)) {
            return chain.filter(exchange);
        }

        String key = KEY_PREFIX + (System.currentTimeMillis() / 60_000);

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofMinutes(1))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > maxRequestsPerMinute) {
                        log.warn("Rate limit exceeded on path {} (count={})", path, count);
                        return rejectWithTooManyRequests(exchange);
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(redisError -> {
                    // If Redis is down, fail open (allow the request) to avoid full outage.
                    log.error("Rate limiter Redis error, failing open: {}", redisError.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> rejectWithTooManyRequests(ServerWebExchange exchange) {
        String body = """
                {"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Maximum %d requests per minute allowed."}
                """.formatted(maxRequestsPerMinute).strip();

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        var buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
