/*
 * 檔案用途：JPA entity，保存 market-data stream durable sequence checkpoint。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketDataSequenceCheckpoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@IdClass(MarketDataSequenceCheckpointRecord.Key.class)
@Table(
        name = "market_data_sequence_checkpoints",
        indexes = {
                @Index(name = "idx_md_seq_symbol_stream", columnList = "symbol,stream")
        }
)
public class MarketDataSequenceCheckpointRecord {

    @Id
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Id
    @Column(name = "stream", nullable = false, length = 64)
    private String stream;

    @Column(name = "sequence_value", nullable = false)
    private Long sequence;

    @Column(name = "checksum", nullable = false)
    private Long checksum;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static MarketDataSequenceCheckpointRecord from(MarketDataSequenceCheckpoint checkpoint) {
        MarketDataSequenceCheckpointRecord record = new MarketDataSequenceCheckpointRecord();
        record.setSymbol(checkpoint.symbol());
        record.setStream(checkpoint.stream());
        record.setSequence(checkpoint.sequence());
        record.setChecksum(checkpoint.checksum());
        record.setUpdatedAt(checkpoint.updatedAt());
        return record;
    }

    public MarketDataSequenceCheckpoint toCheckpoint() {
        return new MarketDataSequenceCheckpoint(symbol, stream, sequence, checksum, updatedAt);
    }

    @Getter
    @Setter
    public static class Key implements Serializable {
        private String symbol;
        private String stream;
    }
}
