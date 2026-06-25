/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.dto.OutboxEvent;
import com.example.exchange.domain.repository.OutboxRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class RedisOutboxRepository implements OutboxRepository {

    private static final String INDEX_KEY = "outbox:index";

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisOutboxRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    @Override
    public void save(OutboxEvent event) {
        String key = eventKey(event.getId());
        boolean isNew = redis.opsForValue().get(key) == null;
        redis.opsForValue().set(key, event);
        if (isNew) {
            redis.opsForList().rightPush(keys.key(INDEX_KEY), event.getId().toString());
        }
    }

    @Override
    public Optional<OutboxEvent> findById(UUID id) {
        return Optional.ofNullable(redis.opsForValue().get(eventKey(id)))
                .filter(OutboxEvent.class::isInstance)
                .map(OutboxEvent.class::cast);
    }

    @Override
    public List<OutboxEvent> findDue(Instant now, int limit) {
        List<Object> ids = redis.opsForList().range(keys.key(INDEX_KEY), 0, -1);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .map(id -> redis.opsForValue().get(eventKey(UUID.fromString(id))))
                .filter(OutboxEvent.class::isInstance)
                .map(OutboxEvent.class::cast)
                .filter(event -> event.getStatus() == OutboxEvent.Status.PENDING)
                .filter(event -> event.getNextAttemptAt() == null || !event.getNextAttemptAt().isAfter(now))
                .filter(Objects::nonNull)
                .limit(Math.max(1, limit))
                .toList();
    }

    private String eventKey(UUID id) {
        return keys.key("outbox:event:" + id);
    }
}
