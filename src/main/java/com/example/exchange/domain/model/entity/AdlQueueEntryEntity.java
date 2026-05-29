/*
 * 檔案用途：JPA entity，保存 ADL liquidation shortfall queue 與 operator claim 狀態。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.AdlQueueEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "adl_queue_entries",
        indexes = {
                @Index(name = "idx_adl_queue_status_time", columnList = "status,created_at"),
                @Index(name = "idx_adl_queue_owner_status", columnList = "owner,status"),
                @Index(name = "idx_adl_queue_symbol_status", columnList = "symbol,status")
        }
)
public class AdlQueueEntryEntity {

    @Id
    @Column(name = "liquidation_id", nullable = false, length = 128)
    private String liquidationId;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "liquidated_side", nullable = false, length = 16)
    private String liquidatedSide;

    @Column(name = "amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "owner", nullable = false, length = 128)
    private String owner;

    @Column(name = "claimed_at", columnDefinition = "DATETIME(6)")
    private Instant claimedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static AdlQueueEntryEntity from(AdlQueueEntry entry, Instant now) {
        AdlQueueEntryEntity entity = new AdlQueueEntryEntity();
        entity.setLiquidationId(entry.liquidationId());
        entity.setUid(entry.uid());
        entity.setSymbol(entry.symbol());
        entity.setLiquidatedSide(entry.liquidatedSide());
        entity.setAmount(entry.amount());
        entity.setStatus(entry.status());
        entity.setOwner(entry.owner());
        entity.setClaimedAt(entry.claimedAt());
        entity.setCreatedAt(entry.ts() == null ? now : entry.ts());
        entity.setUpdatedAt(now);
        return entity;
    }

    public AdlQueueEntry toEntry() {
        return new AdlQueueEntry(
                liquidationId,
                uid,
                symbol,
                liquidatedSide,
                amount,
                createdAt,
                status,
                owner,
                claimedAt
        );
    }
}
