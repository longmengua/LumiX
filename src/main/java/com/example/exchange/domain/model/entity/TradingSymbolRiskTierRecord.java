/*
 * 檔案用途：
 * JPA entity，對應資料表 trading_symbol_risk_tier。
 *
 * 白話：
 * 這張表是合約的槓桿分層。
 *
 * 例如：
 * BTCUSDT-PERP
 * - 小倉位可以 100 倍
 * - 中倉位只能 50 倍
 * - 大倉位只能 20 倍
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.SymbolConfig;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "trading_symbol_risk_tier")
public class TradingSymbolRiskTierRecord {

    /**
     * 資料庫流水 ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 交易對代碼。
     *
     * 例如 BTCUSDT-PERP。
     */
    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    /**
     * 第幾層。
     *
     * 例如：
     * 1、2、3。
     */
    @Column(name = "tier", nullable = false)
    private Integer tier;

    /**
     * 這一層最多可以持有多少倉位金額。
     *
     * 例如：
     * 100000 代表最多 100,000 USDT。
     */
    @Column(name = "max_position_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxPositionNotional;

    /**
     * 這一層最多可以開幾倍槓桿。
     */
    @Column(name = "max_leverage", nullable = false)
    private Integer maxLeverage;

    /**
     * 這一層的初始保證金率。
     *
     * 例如：
     * 100 倍 = 0.01。
     */
    @Column(name = "initial_margin_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal initialMarginRate;

    /**
     * 這一層的維持保證金率。
     */
    @Column(name = "maintenance_margin_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal maintenanceMarginRate;

    /**
     * 建立時間。
     *
     * 由資料庫自動寫入。
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    /**
     * 更新時間。
     *
     * 由資料庫自動更新。
     */
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    /**
     * 轉成 SymbolConfig 裡面使用的 RiskTier。
     */
    public SymbolConfig.RiskTier toRiskTier() {
        return SymbolConfig.RiskTier.builder()
                .tier(tier)
                .maxPositionNotional(maxPositionNotional)
                .maxLeverage(maxLeverage)
                .initialMarginRate(initialMarginRate)
                .maintenanceMarginRate(maintenanceMarginRate)
                .build();
    }

    /**
     * 從 SymbolConfig.RiskTier 轉成資料庫 entity。
     */
    public static TradingSymbolRiskTierRecord from(String symbol, SymbolConfig.RiskTier tier) {
        TradingSymbolRiskTierRecord record = new TradingSymbolRiskTierRecord();

        record.setSymbol(normalize(symbol));
        record.setTier(tier.getTier());
        record.setMaxPositionNotional(tier.getMaxPositionNotional());
        record.setMaxLeverage(tier.getMaxLeverage());
        record.setInitialMarginRate(tier.getInitialMarginRate());
        record.setMaintenanceMarginRate(tier.getMaintenanceMarginRate());

        return record;
    }

    /**
     * 統一把交易對整理成大寫。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}