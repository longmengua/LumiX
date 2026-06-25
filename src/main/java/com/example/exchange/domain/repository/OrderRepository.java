/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order 的持久化抽象 (Repository)
 *
 * 🔑 設計原則：
 * - 這是一個「領域層介面」，只定義要做什麼，**不管怎麼做**。
 * - 真正的實作會放在 infra，例如：
 *   - RedisOrderRepository  → Redis 為主，適合低延遲讀寫（撮合即時狀態）
 *   - JpaOrderRepository    → MySQL/JPA，適合長期存檔與歷史查詢
 * - 撮合邏輯（價格撮合、成交匹配）不會出現在這裡，這裡單純是「CRUD + 查詢」
 */
public interface OrderRepository {

    /**
     * 依 UUID 查詢訂單
     *
     * @param id 訂單唯一 ID
     * @return 如果存在，回傳 Order；否則回傳 Optional.empty()
     *
     * 實作範例：
     * - Redis: HGET order:{id}
     * - MySQL: SELECT * FROM t_order WHERE id = ?
     */
    Optional<Order> findById(UUID id);

    /**
     * 儲存訂單（新增或更新）
     *
     * @param order 訂單物件
     *
     * 實作範例：
     * - Redis: HSET order:{id} {...}
     * - MySQL: INSERT ... ON DUPLICATE KEY UPDATE ...
     *
     * 注意：
     * - 撮合過程中，訂單數量、狀態會不斷更新，必須保持一致性。
     */
    void save(Order order);

    /**
     * 查詢使用者所有「未完成」的訂單
     *
     * @param uid 使用者 ID
     * @return 該用戶所有狀態為 NEW / PARTIALLY_FILLED 的訂單
     *
     * 實作範例：
     * - Redis: ZRANGE user:{uid}:open_orders
     * - MySQL: SELECT * FROM t_order WHERE uid = ? AND status IN ('NEW','PARTIALLY_FILLED')
     */
    List<Order> openOrders(long uid);

    /**
     * 查詢所有使用者尚未完成的訂單。
     *
     * <p>啟動時重建 in-memory order book 會用到這個全域 open-order 視圖，避免 Redis
     * 仍有掛單但 matching engine runtime 是空的。</p>
     */
    default List<Order> openOrders() {
        return List.of();
    }

    /**
     * 查詢使用者在指定交易對的「未完成」訂單
     *
     * @param uid 使用者 ID
     * @param symbol 交易對（例如 BTCUSDT）
     * @return 該用戶在該交易對下的 NEW / PARTIALLY_FILLED 訂單
     */
    List<Order> findOpenOrders(Long uid, String symbol);

    /**
     * 查詢使用者在指定交易對的「所有」訂單（包含歷史紀錄）
     *
     * @param uid 使用者 ID
     * @param symbol 交易對（例如 BTCUSDT）
     * @return 該用戶在該交易對下的所有訂單（已成交、已取消、進行中）
     *
     * 典型用途：
     * - 提供前端「訂單歷史查詢」功能
     * - 對齊 Binance API 的 GET /fapi/v1/allOrders
     */
    List<Order> findAllOrders(Long uid, String symbol);
}
