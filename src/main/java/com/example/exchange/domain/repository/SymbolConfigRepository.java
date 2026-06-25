/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.dto.SymbolConfig;

import java.util.List;
import java.util.Optional;

public interface SymbolConfigRepository {

    Optional<SymbolConfig> findBySymbol(String symbol);

    /**
     * Persist the runtime market configuration after an operator change.
     * Implementations that are read-only should fail clearly instead of silently discarding fee edits.
     */
    default SymbolConfig save(SymbolConfig config) {
        throw new UnsupportedOperationException("symbol config writes are not supported");
    }

    default List<SymbolConfig> findAll() {
        return List.of();
    }
}
