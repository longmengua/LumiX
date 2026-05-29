/*
 * 檔案用途：JPA Repository，提供 Polymarket CLOB command record 查詢與寫入。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.PolymarketClobCommandRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolymarketClobCommandRecordRepository
        extends JpaRepository<PolymarketClobCommandRecordEntity, String> {
}
