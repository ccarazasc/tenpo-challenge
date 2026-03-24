package com.tenpo.challenge.infrastructure.adapter.out.cache;

import com.tenpo.challenge.domain.port.out.PercentageCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis-backed implementation of the percentage cache.
 *
 * Uses a single key with a configurable TTL (default 30 min).
 * Storing as a plain String avoids Jackson serialisation overhead for a scalar value.
 * This adapter works correctly in a multi-replica deployment because Redis is a shared store.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPercentageCacheAdapter implements PercentageCachePort {

    private static final String CACHE_KEY = "tenpo:percentage:latest";

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${app.cache.percentage-ttl-minutes:30}")
    private long ttlMinutes;

    @Override
    public Mono<Double> getPercentage() {
        return redisTemplate.opsForValue()
                .get(CACHE_KEY)
                .map(Double::parseDouble)
                .doOnNext(value -> log.debug("Cache hit for percentage: {}", value))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for percentage key");
                    return Mono.empty();
                }));
    }

    @Override
    public Mono<Void> savePercentage(Double percentage) {
        return redisTemplate.opsForValue()
                .set(CACHE_KEY, String.valueOf(percentage), Duration.ofMinutes(ttlMinutes))
                .doOnSuccess(result -> log.debug("Cached percentage {} for {} minutes", percentage, ttlMinutes))
                .then();
    }
}
