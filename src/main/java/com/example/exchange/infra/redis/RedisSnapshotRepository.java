/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.dto.Snapshot;
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
    private final RedisKeyNamespace keys;

    public RedisSnapshotRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    private String key(long uid) { return keys.key("snap:" + uid); }

    @Override
    public void save(Snapshot snapshot) {
        redis.opsForValue().set(key(snapshot.uid()), snapshot);
    }

    @Override
    public Optional<Snapshot> latest(long uid) {
        return Optional.ofNullable((Snapshot) redis.opsForValue().get(key(uid)));
    }
}
