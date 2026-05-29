/*
 * 檔案用途：ADL queue durable store contract，讓 liquidation shortfall queue 可替換成資料庫持久化。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.AdlQueueEntry;

import java.math.BigDecimal;
import java.util.List;

public interface AdlQueueStore {

    boolean enqueueIfAbsent(AdlQueueEntry entry);

    List<AdlQueueEntry> list();

    boolean complete(String liquidationId);

    AdlQueueEntry updateRemaining(String liquidationId, BigDecimal remainingAmount);

    AdlQueueEntry claim(String liquidationId, String owner);

    AdlQueueEntry release(String liquidationId, String owner);
}
