package com.example.java21_OLAP.infra.redis;

import com.example.java21_OLAP.domain.model.Order;
import com.example.java21_OLAP.domain.repository.OrderRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Repository 的 Redis 實作
 *
 * 設計概念：
 * 1. 每一筆訂單（Order）會存成一個 Redis key
 *    - key 格式：order:{uuid}
 *    - value   ：Order 物件（需序列化，可用 JDK 序列化或 JSON）
 *
 * 2. 每個使用者會有一份「訂單 ID 列表」
 *    - key 格式：ord:{uid}
 *    - value   ：List<String>（訂單 UUID 字串）
 *
 * 優點：
 * - 存取速度快（O(1) 讀寫）
 * - 適合撮合引擎高頻查詢
 *
 * 限制：
 * - Redis 不是長期存儲，適合「快取/即時狀態」
 * - 歷史訂單仍應定期落庫 MySQL
 */
@Repository
public class RedisOrderRepository implements OrderRepository {

    private final RedisTemplate<String, Object> redis;

    public RedisOrderRepository(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /** 使用者訂單列表的 key，例如 ord:1001 */
    private String listKey(long uid) { return "ord:" + uid; }

    /** 單筆訂單的 key，例如 order:550e8400-e29b-41d4-a716-446655440000 */
    private String orderKey(UUID id) { return "order:" + id; }

    /**
     * 依 UUID 查詢訂單
     * - 從 Redis 取出 key = order:{uuid}
     */
    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable((Order) redis.opsForValue().get(orderKey(id)));
    }

    /**
     * 儲存訂單
     * - 1) 把訂單本體存到 Redis (order:{uuid})
     * - 2) 把訂單 ID 放進使用者的訂單列表 (ord:{uid})
     */
    @Override
    public void save(Order o) {
        // 1) 寫單筆
        redis.opsForValue().set(orderKey(o.getId()), o);

        // 2) 確保使用者清單不重複（Redis 7 有 LPOS，可用來判斷是否已存在）
        var uidKey = listKey(o.getUid());
        Long idx = redis.opsForList().indexOf(uidKey, o.getId().toString()); // Spring Data Redis 3.3+ 有 indexOf；若沒有可改用 Lua/Set
        if (idx == null || idx < 0) {
            redis.opsForList().rightPush(uidKey, o.getId().toString());
        }
    }

    /**
     * 查詢使用者的「未完成」訂單
     * - 條件：狀態 != FILLED && != CANCELED
     */
    @Override
    public List<Order> openOrders(long uid) {
        var ids = redis.opsForList().range(listKey(uid), 0, -1);
        if (ids == null) return List.of();
        return ids.stream()
                .map(s -> (Order) redis.opsForValue().get("order:" + s))
                .filter(o -> o != null &&
                        o.getStatus() != Order.Status.FILLED &&
                        o.getStatus() != Order.Status.CANCELED)
                .toList();
    }

    /**
     * 查詢使用者在指定交易對的「未完成」訂單
     */
    @Override
    public List<Order> findOpenOrders(Long uid, String symbol) {
        return openOrders(uid).stream()
                .filter(o -> o.getSymbol().code().equalsIgnoreCase(symbol))
                .collect(Collectors.toList());
    }

    /**
     * 查詢使用者在指定交易對的「所有」訂單（含歷史）
     */
    @Override
    public List<Order> findAllOrders(Long uid, String symbol) {
        var ids = redis.opsForList().range(listKey(uid), 0, -1);
        if (ids == null) return List.of();
        return ids.stream()
                .map(s -> (Order) redis.opsForValue().get("order:" + s))
                .filter(o -> o != null && o.getSymbol().code().equalsIgnoreCase(symbol))
                .toList();
    }
}
