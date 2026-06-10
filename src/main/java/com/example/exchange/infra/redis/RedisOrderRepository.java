/*
 * 檔案用途：Redis Repository 實作，用 Redis 保存低延遲交易狀態與快照資料。
 */
package com.example.exchange.infra.redis; // 套件名稱，放在 infra.redis 命名空間下

import com.example.exchange.domain.model.entity.Order; // 匯入網域模型：訂單
import com.example.exchange.domain.repository.OrderRepository; // 匯入訂單儲存庫介面
import org.springframework.dao.DataAccessException; // 匯入資料存取例外
import org.springframework.data.redis.core.*; // 匯入 Spring Data Redis 相關操作介面
import org.springframework.stereotype.Repository; // 匯入 Repository 註解
import org.springframework.transaction.support.TransactionSynchronizationManager; // 匯入交易同步管理器，用來判斷是否在事務中

import java.util.*; // 匯入 Java 通用工具類別
import java.util.stream.Collectors; // 匯入串流工具

/**
 * RedisOrderRepository
 * -------------------------------------------------
 * 設計重點：
 * 1) 單筆訂單本體：key = order:{uuid}，value = Order 物件
 * 2) 每位用戶的訂單索引：
 *    - 有序清單(List)：key = ord:list:{uid}，保存訂單 ID（字串）以保留插入順序
 *    - 去重集合(Set)：key = ord:set:{uid}，用於檢查是否已存在，避免重複 push 到 List
 * 3) 批量查詢使用 multiGet，減少往返；兼容叢集
 * 4) 自動清理懸掛(dangling) ID（List/Set 都會移除），但避免在 Spring 事務中執行
 * 5) 保留方法語意：openOrders / findOpenOrders / findAllOrders
 */
@Repository
public class RedisOrderRepository implements OrderRepository {

    private final RedisTemplate<String, Object> redis; // 注入的 RedisTemplate，用來與 Redis 互動（key=String, value=Object）
    private final RedisKeyNamespace keys;

    public RedisOrderRepository(RedisTemplate<String, Object> redis, RedisKeyNamespace keys) { // 建構子，透過依賴注入取得 RedisTemplate
        this.redis = redis; // 指派成員變數
        this.keys = keys;
    }

    // ========================= Key 規範 =========================

    private String listKey(long uid) { return keys.key("ord:list:" + uid); } // 取得用戶訂單清單(List)的 key，例如 ord:list:1001

    private String setKey(long uid) { return keys.key("ord:set:" + uid); } // 取得用戶訂單集合(Set)的 key，例如 ord:set:1001

    private String allOrderIdsKey() { return keys.key("ord:all"); } // 全域訂單 ID 集合，用於啟動時重建 matching book

    private String orderKey(UUID id) { return keys.key("order:" + id); } // 取得單筆訂單本體的 key，例如 order:550e8400-e29b-41d4-a716-446655440000

    // ========================= 介面實作 =========================

    @Override
    public Optional<Order> findById(UUID id) { // 依照 UUID 查詢單筆訂單
        Object v = redis.opsForValue().get(orderKey(id)); // 直接透過 ValueOperations 取得值（對應 GET）
        if (!(v instanceof Order)) return Optional.empty(); // 若不是 Order 型別（或為 null），回傳空 Optional
        return Optional.of((Order) v); // 轉型成功則回傳包裝好的 Optional<Order>
    }

    @Override
    public void save(Order o) { // 儲存訂單（本體 + 將 ID 放入用戶索引）
        String orderKey = orderKey(o.getId()); // 準備訂單本體 key
        String uidListKey = listKey(o.getUid()); // 準備用戶 List key
        String uidSetKey = setKey(o.getUid()); // 準備用戶 Set key
        String idStr = o.getId().toString(); // 將 UUID 轉為字串存入索引

        redis.opsForValue().set(orderKey, o); // 1) 先寫入訂單本體（SET order:{uuid} -> Order）
        redis.opsForSet().add(allOrderIdsKey(), idStr); // 全域索引用於 restart recovery；重複 save 由 Set 去重。

        Long added = redis.opsForSet().add(uidSetKey, idStr); // 2) 嘗試加入 Set 做去重（SADD 回傳 1 表示成功新增、0 表示已存在）
        if (added != null && added > 0) { // 如果成功加入 Set（代表先前不存在）
            redis.opsForList().rightPush(uidListKey, idStr); // 才 push 到 List（保序）
        }
    }

    @Override
    public List<Order> openOrders(long uid) { // 查詢使用者所有「未完成」訂單
        List<String> ids = readIdList(uid); // 讀取用戶的訂單 ID 清單（保序）
        if (ids.isEmpty()) return List.of(); // 若沒有任何 ID，直接回傳空集合

        Map<String, Order> map = batchGetOrders(ids); // 批量取得訂單本體（ID -> Order）

        removeDanglingIds(uid, ids, map); // 自動清掉清單中找不到本體的 ID（避免髒資料）

        return map.values().stream()
                .filter(Objects::nonNull) // 移除 null
                .filter(this::isOpen) // 過濾掉已終態訂單
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> openOrders() {
        Set<Object> rawIds = redis.opsForSet().members(allOrderIdsKey());
        List<String> ids = rawIds == null
                ? new ArrayList<>()
                : rawIds.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toCollection(ArrayList::new));
        // 舊版資料可能尚未寫入 ord:all；掃描既有 user list key 作為相容性 fallback。
        if (ids.isEmpty()) {
            ids.addAll(scanOrderIdsFromUserLists());
        }
        if (ids.isEmpty()) return List.of();

        Map<String, Order> map = batchGetOrders(ids.stream().distinct().toList());
        return map.values().stream()
                .filter(Objects::nonNull)
                .filter(this::isOpen)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findOpenOrders(Long uid, String symbol) { // 查詢指定交易對的「未完成」訂單
        String sym = symbol == null ? "" : symbol.trim(); // 安全處理 symbol
        if (sym.isEmpty()) return List.of(); // 空字串直接回傳空集合

        return openOrders(uid).stream() // 先取得所有未完成訂單，再過濾 symbol
                .filter(o -> o.getSymbol() != null && o.getSymbol().code() != null)
                .filter(o -> o.getSymbol().code().equalsIgnoreCase(sym))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAllOrders(Long uid, String symbol) { // 查詢指定交易對的「所有訂單」（含歷史）
        String sym = symbol == null ? "" : symbol.trim(); // 處理 symbol 空白/空值
        if (sym.isEmpty()) return List.of(); // 空字串直接回傳空集合

        List<String> ids = readIdList(uid); // 取得該用戶的所有訂單 ID（保序）
        if (ids.isEmpty()) return List.of(); // 若無 ID 直接回傳空集合

        Map<String, Order> map = batchGetOrders(ids); // 批量取得訂單本體

        removeDanglingIds(uid, ids, map); // 移除失效 ID（List/Set 同步清理）

        return map.values().stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getSymbol() != null && o.getSymbol().code() != null)
                .filter(o -> o.getSymbol().code().equalsIgnoreCase(sym))
                .collect(Collectors.toList());
    }

    // ======================= 私有工具方法 =======================

    @SuppressWarnings("unchecked")
    private List<String> readIdList(long uid) { // 讀取使用者的訂單 ID 清單（List）
        List<Object> raw = redis.opsForList().range(listKey(uid), 0, -1); // 從 Redis 取出整個 List
        if (raw == null || raw.isEmpty()) return List.of();
        List<String> ids = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o == null) continue;
            if (o instanceof String s) ids.add(s);
            else ids.add(String.valueOf(o));
        }
        return ids;
    }

    private Map<String, Order> batchGetOrders(List<String> ids) { // 批量查詢訂單本體
        if (ids.isEmpty()) return Collections.emptyMap();

        List<String> redisKeys = ids.stream().map(id -> keys.key("order:" + id)).collect(Collectors.toList()); // 轉成 order key
        ValueOperations<String, Object> vo = redis.opsForValue();
        List<Object> values = vo.multiGet(redisKeys); // MGET

        Map<String, Order> map = new LinkedHashMap<>(ids.size());
        if (values == null) {
            ids.forEach(id -> map.put(id, null));
            return map;
        }

        for (int i = 0; i < ids.size(); i++) {
            Object v = values.size() > i ? values.get(i) : null;
            map.put(ids.get(i), (v instanceof Order) ? (Order) v : null);
        }
        return map;
    }

    private List<String> scanOrderIdsFromUserLists() {
        Set<String> listKeys = redis.keys(keys.key("ord:list:*"));
        if (listKeys == null || listKeys.isEmpty()) return List.of();
        List<String> ids = new ArrayList<>();
        for (String key : listKeys) {
            List<Object> raw = redis.opsForList().range(key, 0, -1);
            if (raw == null) continue;
            raw.stream().filter(Objects::nonNull).map(String::valueOf).forEach(ids::add);
        }
        return ids;
    }

    private void removeDanglingIds(long uid, List<String> ids, Map<String, Order> id2Order) { // 清除沒有本體的 ID
        if (TransactionSynchronizationManager.isActualTransactionActive()) return; // 若在事務中，跳過清理

        List<String> toRemove = ids.stream().filter(id -> id2Order.get(id) == null).collect(Collectors.toList());
        if (toRemove.isEmpty()) return;

        try {
            BoundListOperations<String, Object> lo = redis.boundListOps(listKey(uid));
            BoundSetOperations<String, Object> so = redis.boundSetOps(setKey(uid));
            for (String id : toRemove) {
                lo.remove(0, id); // 從 List 移除
                so.remove(id); // 從 Set 移除
            }
        } catch (DataAccessException ignored) {
            // 清理失敗不影響主流程
        }
    }

    private boolean isOpen(Order order) {
        return order.getStatus() == Order.Status.NEW
                || order.getStatus() == Order.Status.PARTIALLY_FILLED;
    }
}
