/*
 * 檔案用途：
 * Spring Data JPA repository，操作 trading_symbol 表。
 *
 * 白話：
 * 這支負責查詢、儲存交易對主設定。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.TradingSymbolRecord;
import com.example.exchange.domain.model.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradingSymbolRecordJpaRepository extends JpaRepository<TradingSymbolRecord, Long> {

    /**
     * 用交易對代碼查設定。
     *
     * 例如：
     * BTCUSDT-SPOT
     * BTCUSDT-PERP
     */
    Optional<TradingSymbolRecord> findBySymbol(String symbol);

    /**
     * 查某一種商品類型。
     *
     * 例如：
     * 只查現貨
     * 只查永續合約
     */
    List<TradingSymbolRecord> findByProductTypeOrderBySymbolAsc(ProductType productType);

    /**
     * 查前台可顯示、可交易的交易對。
     *
     * 給前台交易頁使用。
     */
    List<TradingSymbolRecord> findByVisibleTrueAndTradingEnabledTrueOrderBySymbolAsc();

    /**
     * 查某一種商品類型底下，前台可顯示、可交易的交易對。
     *
     * 例如：
     * 現貨頁只拿 SPOT。
     * 合約頁只拿 PERPETUAL。
     */
    List<TradingSymbolRecord> findByProductTypeAndVisibleTrueAndTradingEnabledTrueOrderBySymbolAsc(ProductType productType);

    /**
     * 用 base / quote 查。
     *
     * 例如：
     * baseAsset = BTC
     * quoteAsset = USDT
     */
    List<TradingSymbolRecord> findByBaseAssetAndQuoteAssetOrderBySymbolAsc(String baseAsset, String quoteAsset);

    /**
     * 檢查交易對是否存在。
     */
    boolean existsBySymbol(String symbol);
}