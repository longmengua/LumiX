package com.example.java21_OLAP.infra.redis;

import com.example.java21_OLAP.domain.model.Position;
import com.example.java21_OLAP.domain.model.Symbol;
import com.example.java21_OLAP.domain.repository.PositionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Position Repository 的 Redis 實作
 * - 使用 Hash：key = pos:{uid}，field = symbolCode，value = Position 物件
 */
@Repository
public class RedisPositionRepository implements PositionRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisPositionRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    private String key(long uid) {
        return "pos:" + uid;
    }

    @Override
    public Optional<Position> find(long uid, Symbol symbol) {
        Map<Object, Object> map = redis.opsForHash().entries(key(uid));
        return Optional.ofNullable((Position) map.get(symbol.code()));
    }

    @Override
    public void save(Position p) {
        redis.opsForHash().put(key(p.getUid()), p.getSymbol().code(), p);
    }

    @Override
    public List<Position> findAllByUid(long uid) {
        return redis.opsForHash().values(key(uid)).stream()
                .map(o -> (Position) o)
                .toList();
    }
}
