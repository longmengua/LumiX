/*
 * 檔案用途：JPA Repository，提供 MySQL 持久化查詢與寫入能力。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionPolymarketOrderRepository
        extends JpaRepository<PredictionPolymarketOrder, Long> {

    Optional<PredictionPolymarketOrder> findByInternalOrderId(String internalOrderId);

    Optional<PredictionPolymarketOrder> findByClobOrderId(String clobOrderId);

    List<PredictionPolymarketOrder> findByStatusInOrderByIdAsc(List<String> statuses);
}
