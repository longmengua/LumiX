/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis;

import com.example.exchange.domain.model.dto.Position;
import com.example.exchange.domain.model.dto.Symbol;
import com.example.exchange.domain.repository.PositionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Position Repository 的 Redis 實作
 * - 使用 Hash：key = pos:{uid}，field = symbolCode，value = Position 物件
 */
@Repository
public class RedisPositionRepository implements PositionRepository {

    private static final String OPEN_INDEX_KEY = "pos:open:index";

    private final RedisTemplate<String, Object> redis;
    private final RedisKeyNamespace keys;

    public RedisPositionRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) {
        this.redis = redis;
        this.keys = keys;
    }

    private String key(long uid) {
        return keys.key("pos:" + uid);
    }

    @Override
    public Optional<Position> find(long uid, Symbol symbol) {
        Map<Object, Object> map = redis.opsForHash().entries(key(uid));
        return Optional.ofNullable((Position) map.get(symbol.code()));
    }

    @Override
    public void save(Position p) {
        redis.opsForHash().put(key(p.getUid()), p.getSymbol().code(), p);
        String member = openIndexMember(p.getUid(), p.getSymbol().code());
        if (p.getQty() != null && p.getQty().signum() != 0) {
            redis.opsForSet().add(keys.key(OPEN_INDEX_KEY), member);
        } else {
            redis.opsForSet().remove(keys.key(OPEN_INDEX_KEY), member);
        }
    }

    @Override
    public List<Position> findAllByUid(long uid) {
        return redis.opsForHash().values(key(uid)).stream()
                .map(o -> (Position) o)
                .toList();
    }

    @Override
    public List<Position> findOpenPositions() {
        Set<Object> members = redis.opsForSet().members(keys.key(OPEN_INDEX_KEY));
        if (members == null || members.isEmpty()) return List.of();
        return members.stream()
                .map(String::valueOf)
                .map(this::loadOpenPosition)
                .filter(Position.class::isInstance)
                .map(Position.class::cast)
                .filter(position -> position.getQty() != null && position.getQty().signum() != 0)
                .toList();
    }

    private Position loadOpenPosition(String member) {
        int separator = member.indexOf(':');
        if (separator <= 0 || separator == member.length() - 1) return null;
        long uid;
        try {
            uid = Long.parseLong(member.substring(0, separator));
        } catch (NumberFormatException ex) {
            redis.opsForSet().remove(keys.key(OPEN_INDEX_KEY), member);
            return null;
        }
        String symbol = member.substring(separator + 1);
        Object value = redis.opsForHash().get(key(uid), symbol);
        if (value == null) {
            redis.opsForSet().remove(keys.key(OPEN_INDEX_KEY), member);
            return null;
        }
        return value instanceof Position position ? position : null;
    }

    private static String openIndexMember(long uid, String symbol) {
        return uid + ":" + symbol;
    }
}
