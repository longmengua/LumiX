package com.example.exchange.infra.redis;

import com.example.exchange.domain.repository.IdempotencyRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

@Repository
public class RedisIdempotencyRepository implements IdempotencyRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisIdempotencyRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    @Override
    public boolean insertIfAbsent(String key, Instant expiresAt) {
        String redisKey = key(key);
        if (expiresAt == null) {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(redisKey, "1"));
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(1);
        }
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(redisKey, "1", ttl));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key(key)));
    }

    private static String key(String key) {
        return "idempotency:" + key;
    }
}
