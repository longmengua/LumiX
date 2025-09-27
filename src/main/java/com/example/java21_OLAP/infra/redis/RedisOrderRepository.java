package com.example.java21_OLAP.infra.redis;

import com.example.java21_OLAP.domain.model.Order;
import com.example.java21_OLAP.domain.repository.OrderRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository 的 Redis 實作
 * - 用 String 保存單筆訂單資料：key = order:{uuid}
 * - 用 List 保存使用者訂單 id 列表：key = ord:{uid}
 */
@Repository
public class RedisOrderRepository implements OrderRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisOrderRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    private String listKey(long uid) { return "ord:" + uid; }
    private String orderKey(UUID id) { return "order:" + id; }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable((Order) redis.opsForValue().get(orderKey(id)));
    }

    @Override
    public void save(Order o) {
        redis.opsForValue().set(orderKey(o.id()), o);
        redis.opsForList().rightPush(listKey(o.uid()), o.id().toString());
    }

    @Override
    public List<Order> openOrders(long uid) {
        var ids = redis.opsForList().range(listKey(uid), 0, -1);
        if (ids == null) return List.of();
        return ids.stream()
                .map(s -> (Order) redis.opsForValue().get("order:" + s))
                .filter(o -> o != null && o.status() != Order.Status.FILLED)
                .toList();
    }
}
