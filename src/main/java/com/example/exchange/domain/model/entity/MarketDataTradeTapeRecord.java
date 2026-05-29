/*
 * 檔案用途：JPA entity，保存 market-data trade tape。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.enums.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "market_data_trade_tape",
        indexes = {
                @Index(name = "idx_md_trade_symbol_time", columnList = "symbol,trade_ts,id"),
                @Index(name = "idx_md_trade_match", columnList = "match_id")
        }
)
public class MarketDataTradeTapeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "match_id", nullable = false, length = 128)
    private String matchId;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 16)
    private OrderSide side;

    @Column(name = "price", nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "qty", nullable = false, precision = 38, scale = 18)
    private BigDecimal qty;

    @Column(name = "maker", nullable = false)
    private Boolean maker;

    @Column(name = "trade_ts", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant tradeTs;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static MarketDataTradeTapeRecord from(TradeTapeItem item) {
        MarketDataTradeTapeRecord record = new MarketDataTradeTapeRecord();
        record.setSymbol(normalize(item.symbol()));
        record.setMatchId(item.matchId());
        record.setOrderId(item.orderId() == null ? null : item.orderId().toString());
        record.setSide(item.side());
        record.setPrice(item.price());
        record.setQty(item.qty());
        record.setMaker(item.maker());
        record.setTradeTs(item.ts());
        record.setCreatedAt(Instant.now());
        return record;
    }

    public TradeTapeItem toItem() {
        return new TradeTapeItem(
                symbol,
                matchId,
                orderId == null ? null : UUID.fromString(orderId),
                side,
                price,
                qty,
                Boolean.TRUE.equals(maker),
                tradeTs
        );
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
