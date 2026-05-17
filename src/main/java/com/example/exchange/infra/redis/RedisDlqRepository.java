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

    public RedisDlqRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    @Override
    public void append(DlqEvent event) {
        redis.opsForValue().set(eventKey(event.getId().toString()), event);
        redis.opsForList().leftPush(INDEX_KEY, event.getId().toString());
    }

    @Override
    public List<DlqEvent> latest(int limit) {
        List<Object> ids = redis.opsForList().range(INDEX_KEY, 0, Math.max(1, limit) - 1L);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(String::valueOf)
                .map(id -> redis.opsForValue().get(eventKey(id)))
                .filter(DlqEvent.class::isInstance)
                .map(DlqEvent.class::cast)
                .toList();
    }

    private static String eventKey(String id) {
        return "dlq:event:" + id;
    }
}
