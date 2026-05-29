/*
 * 檔案用途：JPA entity，保存 market-data depth delta 供 reconnect backfill 使用。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@IdClass(MarketDataDepthDeltaRecord.Key.class)
@Table(
        name = "market_data_depth_deltas",
        indexes = {
                @Index(name = "idx_md_depth_symbol_version", columnList = "symbol,version_value")
        }
)
public class MarketDataDepthDeltaRecord {

    @Id
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Id
    @Column(name = "version_value", nullable = false)
    private Long version;

    @Column(name = "checksum", nullable = false)
    private Long checksum;

    @Lob
    @Column(name = "bids_json", nullable = false, columnDefinition = "LONGTEXT")
    private String bidsJson;

    @Lob
    @Column(name = "asks_json", nullable = false, columnDefinition = "LONGTEXT")
    private String asksJson;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Getter
    @Setter
    public static class Key implements Serializable {
        private String symbol;
        private Long version;
    }
}
