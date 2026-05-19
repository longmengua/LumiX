/*
 * 檔案用途：Repository 介面，定義 order lifecycle projection 的查詢與寫入契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.OrderLifecycleProjection;

import java.util.List;
import java.util.Optional;

public interface OrderLifecycleProjectionStore {

    void save(OrderLifecycleProjection projection);

    Optional<OrderLifecycleProjection> findByOrderId(String orderId);

    List<OrderLifecycleProjection> findByUid(long uid);

    List<OrderLifecycleProjection> findByUidAndSymbol(long uid, String symbol);

    Optional<OrderLifecycleProjection> findByClientOrderId(String clientOrderId);
}
