/*
 * 檔案用途：JPA entity，保存 market-data kline records。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketKline;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@IdClass(MarketDataKlineRecord.Key.class)
@Table(
        name = "market_data_klines",
        indexes = {
                @Index(name = "idx_md_kline_symbol_interval_time", columnList = "symbol,interval_value,open_time")
        }
)
public class MarketDataKlineRecord {

    @Id
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Id
    @Column(name = "interval_value", nullable = false, length = 16)
    private String interval;

    @Id
    @Column(name = "open_time", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant openTime;

    @Column(name = "open_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal open;

    @Column(name = "high_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal high;

    @Column(name = "low_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal low;

    @Column(name = "close_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal close;

    @Column(name = "volume", nullable = false, precision = 38, scale = 18)
    private BigDecimal volume;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static MarketDataKlineRecord from(MarketKline kline) {
        MarketDataKlineRecord record = new MarketDataKlineRecord();
        record.setSymbol(normalize(kline.symbol()));
        record.setInterval(normalizeInterval(kline.interval()));
        record.setOpenTime(kline.openTime());
        record.setOpen(kline.open());
        record.setHigh(kline.high());
        record.setLow(kline.low());
        record.setClose(kline.close());
        record.setVolume(kline.volume());
        record.setUpdatedAt(Instant.now());
        return record;
    }

    public MarketKline toKline() {
        return new MarketKline(symbol, interval, openTime, open, high, low, close, volume);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private static String normalizeInterval(String interval) {
        return interval == null ? "" : interval.trim().toLowerCase();
    }

    @Getter
    @Setter
    public static class Key implements Serializable {
        private String symbol;
        private String interval;
        private Instant openTime;
    }
}
