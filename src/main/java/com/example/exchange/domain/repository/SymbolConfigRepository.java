/*
 * 檔案用途：Repository 介面，定義領域層需要的資料存取契約。
 */
package com.example.exchange.domain.repository;

import com.example.exchange.domain.model.entity.SymbolConfig;

import java.util.List;
import java.util.Optional;

public interface SymbolConfigRepository {

    Optional<SymbolConfig> findBySymbol(String symbol);

    default List<SymbolConfig> findAll() {
        return List.of();
    }
}
