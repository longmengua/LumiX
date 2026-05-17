package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.entity.OutboxEvent;
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

    public RedisOutboxRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    @Override
    public void save(OutboxEvent event) {
        String key = eventKey(event.getId());
        boolean isNew = redis.opsForValue().get(key) == null;
        redis.opsForValue().set(key, event);
        if (isNew) {
            redis.opsForList().rightPush(INDEX_KEY, event.getId().toString());
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
        List<Object> ids = redis.opsForList().range(INDEX_KEY, 0, -1);
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

    private static String eventKey(UUID id) {
        return "outbox:event:" + id;
    }
}
