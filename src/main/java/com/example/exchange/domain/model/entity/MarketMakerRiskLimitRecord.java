/*
 * 檔案用途：JPA entity，保存做市商 per-symbol risk limit。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "market_maker_risk_limits",
        indexes = {
                @Index(name = "idx_market_maker_risk_limits_mm", columnList = "market_maker_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_market_maker_risk_limit_symbol",
                        columnNames = {"market_maker_id", "symbol"}
                )
        }
)
public class MarketMakerRiskLimitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_maker_id", nullable = false, length = 128)
    private String marketMakerId;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "max_long_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxLongNotional;

    @Column(name = "max_short_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxShortNotional;

    @Column(name = "max_order_notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxOrderNotional;

    @Column(name = "max_slippage_rate", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxSlippageRate;

    @Column(name = "kill_switch", nullable = false)
    private Boolean killSwitch;

    public static MarketMakerRiskLimitRecord from(String marketMakerId, MarketMakerRiskLimit limit) {
        MarketMakerRiskLimitRecord record = new MarketMakerRiskLimitRecord();
        record.setMarketMakerId(marketMakerId);
        record.setSymbol(limit.symbol());
        record.setMaxLongNotional(limit.maxLongNotional());
        record.setMaxShortNotional(limit.maxShortNotional());
        record.setMaxOrderNotional(limit.maxOrderNotional());
        record.setMaxSlippageRate(limit.maxSlippageRate());
        record.setKillSwitch(limit.killSwitch());
        return record;
    }

    public MarketMakerRiskLimit toLimit() {
        return new MarketMakerRiskLimit(
                symbol,
                maxLongNotional,
                maxShortNotional,
                maxOrderNotional,
                maxSlippageRate,
                killSwitch
        );
    }
}
