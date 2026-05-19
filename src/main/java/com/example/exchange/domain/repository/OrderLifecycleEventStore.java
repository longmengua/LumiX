/*
 * 檔案用途：Repository 介面，定義 order lifecycle durable event log 的存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;

import java.util.List;

public interface OrderLifecycleEventStore {

    void append(OrderLifecycleEventRecord record);

    List<OrderLifecycleEventRecord> findByOrderId(String orderId);
}
