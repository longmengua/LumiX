/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.entity.DlqEvent;
import com.example.exchange.domain.repository.DlqRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RedisDlqRepository implements DlqRepository {

    private static final String INDEX_KEY = "dlq:index";

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisDlqRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    @Override
    public void append(DlqEvent event) {
        redis.opsForValue().set(eventKey(event.getId().toString()), event);
        redis.opsForList().leftPush(keys.key(INDEX_KEY), event.getId().toString());
    }

    @Override
    public List<DlqEvent> latest(int limit) {
        List<Object> ids = redis.opsForList().range(keys.key(INDEX_KEY), 0, Math.max(1, limit) - 1L);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(String::valueOf)
                .map(id -> redis.opsForValue().get(eventKey(id)))
                .filter(DlqEvent.class::isInstance)
                .map(DlqEvent.class::cast)
                .toList();
    }

    private String eventKey(String id) {
        return keys.key("dlq:event:" + id);
    }
}
