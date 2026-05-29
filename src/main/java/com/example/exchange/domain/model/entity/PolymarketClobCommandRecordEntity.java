/*
 * 檔案用途：JPA entity，保存 Polymarket CLOB command idempotency claim/result。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.PolymarketClobCommandRecord;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "polymarket_clob_command_record",
        indexes = {
                @Index(name = "idx_poly_clob_command_order", columnList = "internal_order_id"),
                @Index(name = "idx_poly_clob_command_type_completed", columnList = "command_type,completed")
        }
)
public class PolymarketClobCommandRecordEntity {

    @Id
    @Column(name = "command_id", nullable = false, length = 128)
    private String commandId;

    @Column(name = "command_type", nullable = false, length = 32)
    private String commandType;

    @Column(name = "internal_order_id", nullable = false, length = 64)
    private String internalOrderId;

    @Column(name = "fingerprint", nullable = false, length = 512)
    private String fingerprint;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "result_status", length = 64)
    private String resultStatus;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static PolymarketClobCommandRecordEntity claimed(
            String commandId,
            String commandType,
            String internalOrderId,
            String fingerprint,
            Instant now
    ) {
        PolymarketClobCommandRecordEntity entity =
                new PolymarketClobCommandRecordEntity();
        entity.setCommandId(commandId);
        entity.setCommandType(commandType);
        entity.setInternalOrderId(internalOrderId);
        entity.setFingerprint(fingerprint);
        entity.setCompleted(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public PolymarketClobCommandRecord toRecord() {
        return new PolymarketClobCommandRecord(
                commandId,
                commandType,
                internalOrderId,
                fingerprint,
                Boolean.TRUE.equals(completed),
                resultStatus,
                lastError
        );
    }
}
