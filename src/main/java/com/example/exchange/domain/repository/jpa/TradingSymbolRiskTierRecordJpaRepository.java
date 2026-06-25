/*
 * 檔案用途：
 * Spring Data JPA repository，操作 trading_symbol_risk_tier 表。
 *
 * 白話：
 * 這支負責查詢、儲存合約槓桿分層。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.TradingSymbolRiskTierRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TradingSymbolRiskTierRecordJpaRepository extends JpaRepository<TradingSymbolRiskTierRecord, Long> {

    /**
     * 查某個交易對的風控分層。
     *
     * 例如：
     * BTCUSDT-PERP 的 100 倍、50 倍、20 倍分層。
     */
    List<TradingSymbolRiskTierRecord> findBySymbolOrderByMaxPositionNotionalAsc(String symbol);

    /**
     * 一次查多個交易對的風控分層。
     *
     * 之後 findAll symbol config 時可以避免一個交易對查一次 DB。
     */
    List<TradingSymbolRiskTierRecord> findBySymbolInOrderBySymbolAscMaxPositionNotionalAsc(Collection<String> symbols);

    /**
     * 後台重設某個交易對的 risk tiers 時使用。
     */
    void deleteBySymbol(String symbol);
}