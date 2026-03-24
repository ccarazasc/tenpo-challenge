package com.tenpo.challenge.infrastructure.adapter.out.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisPercentageCacheAdapter")
class RedisPercentageCacheAdapterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisPercentageCacheAdapter cacheAdapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Inject ttlMinutes = 30 via reflection
        try {
            var field = RedisPercentageCacheAdapter.class.getDeclaredField("ttlMinutes");
            field.setAccessible(true);
            field.set(cacheAdapter, 30L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("returns cached percentage when key exists in Redis")
    void getPercentage_hit() {
        when(valueOps.get(anyString())).thenReturn(Mono.just("10.5"));

        StepVerifier.create(cacheAdapter.getPercentage())
                .expectNext(10.5)
                .verifyComplete();
    }

    @Test
    @DisplayName("returns empty Mono when key does not exist (cache miss)")
    void getPercentage_miss() {
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cacheAdapter.getPercentage())
                .verifyComplete();
    }

    @Test
    @DisplayName("saves percentage as string with configured TTL")
    void savePercentage_storesWithTtl() {
        when(valueOps.set(anyString(), eq("10.0"), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheAdapter.savePercentage(10.0))
                .verifyComplete();

        verify(valueOps).set(anyString(), eq("10.0"), eq(Duration.ofMinutes(30)));
    }
}
