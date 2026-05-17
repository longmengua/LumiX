/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.repository.IdempotencyRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

@Repository
public class RedisIdempotencyRepository implements IdempotencyRepository {

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisIdempotencyRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
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

    private String key(String key) {
        return keys.key("idempotency:" + key);
    }
}
