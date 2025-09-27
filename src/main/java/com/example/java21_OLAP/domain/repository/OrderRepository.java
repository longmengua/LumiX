package com.example.java21_OLAP.domain.repository;

import com.example.java21_OLAP.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order 的持久化抽象
 *
 * 注意：撮合邏輯/訂單簿不在此介面，這裡只負責訂單資料的 CRUD 與查詢。
 */
public interface OrderRepository {

    /** 依 UUID 取得訂單 */
    Optional<Order> findById(UUID id);

    /** 儲存（新增或更新）訂單 */
    void save(Order order);

    /** 查詢使用者的尚未成交完成之訂單（狀態 NEW / PARTIALLY_FILLED） */
    List<Order> openOrders(long uid);
}
