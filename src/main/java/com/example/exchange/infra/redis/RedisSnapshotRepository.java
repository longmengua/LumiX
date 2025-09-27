package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.Snapshot;
import com.example.exchange.domain.repository.SnapshotRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Snapshot Repository 的 Redis 實作
 * - 簡單以 String key 存最新快照：key = snap:{uid}
 */
@Repository
public class RedisSnapshotRepository implements SnapshotRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisSnapshotRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    private String key(long uid) { return "snap:" + uid; }

    @Override
    public void save(Snapshot snapshot) {
        redis.opsForValue().set(key(snapshot.uid()), snapshot);
    }

    @Override
    public Optional<Snapshot> latest(long uid) {
        return Optional.ofNullable((Snapshot) redis.opsForValue().get(key(uid)));
    }
}
