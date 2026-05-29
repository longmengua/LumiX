/*
 * 檔案用途：JPA entity，保存 market-data ticker latest state。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketTicker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "market_data_tickers")
public class MarketDataTickerRecord {

    @Id
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "last_price", precision = 38, scale = 18)
    private BigDecimal lastPrice;

    @Column(name = "best_bid", precision = 38, scale = 18)
    private BigDecimal bestBid;

    @Column(name = "best_ask", precision = 38, scale = 18)
    private BigDecimal bestAsk;

    @Column(name = "volume_24h", nullable = false, precision = 38, scale = 18)
    private BigDecimal volume24h;

    @Column(name = "high_24h", precision = 38, scale = 18)
    private BigDecimal high24h;

    @Column(name = "low_24h", precision = 38, scale = 18)
    private BigDecimal low24h;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static MarketDataTickerRecord from(MarketTicker ticker) {
        MarketDataTickerRecord record = new MarketDataTickerRecord();
        record.setSymbol(normalize(ticker.symbol()));
        record.setLastPrice(ticker.lastPrice());
        record.setBestBid(ticker.bestBid());
        record.setBestAsk(ticker.bestAsk());
        record.setVolume24h(ticker.volume24h() == null ? BigDecimal.ZERO : ticker.volume24h());
        record.setHigh24h(ticker.high24h());
        record.setLow24h(ticker.low24h());
        record.setUpdatedAt(ticker.updatedAt() == null ? Instant.now() : ticker.updatedAt());
        return record;
    }

    public MarketTicker toTicker() {
        return new MarketTicker(symbol, lastPrice, bestBid, bestAsk, volume24h, high24h, low24h, updatedAt);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
