/*
 * 檔案用途：JPA entity，保存做市商 hedge fill audit trail。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.enums.OrderSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "hedge_fills",
        indexes = {
                @Index(name = "idx_hedge_fills_mm_time", columnList = "market_maker_id,filled_at"),
                @Index(name = "idx_hedge_fills_venue_order", columnList = "venue_order_id"),
                @Index(name = "idx_hedge_fills_ref", columnList = "ref_id"),
                @Index(name = "idx_hedge_fills_ledger_ref", columnList = "ledger_ref_id"),
                @Index(name = "idx_hedge_fills_symbol_time", columnList = "symbol,filled_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hedge_fills_venue_fill",
                        columnNames = {"venue_order_id", "venue_fill_id"}
                )
        }
)
public class HedgeFillRecordEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "market_maker_id", nullable = false, length = 128)
    private String marketMakerId;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "venue_order_id", nullable = false, length = 128)
    private String venueOrderId;

    @Column(name = "venue_fill_id", nullable = false, length = 128)
    private String venueFillId;

    @Column(name = "side", nullable = false, length = 16)
    private String side;

    @Column(name = "quantity", nullable = false, precision = 38, scale = 18)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "fee", nullable = false, precision = 38, scale = 18)
    private BigDecimal fee;

    @Column(name = "fee_asset", length = 32)
    private String feeAsset;

    @Column(name = "ref_id", length = 128)
    private String refId;

    @Column(name = "ledger_ref_id", length = 128)
    private String ledgerRefId;

    @Column(name = "filled_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant filledAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static HedgeFillRecordEntity from(HedgeFillRecord record, int schemaVersion) {
        HedgeFillRecordEntity entity = new HedgeFillRecordEntity();
        entity.setId(record.id().toString());
        entity.setSchemaVersion(schemaVersion);
        entity.setMarketMakerId(record.marketMakerId());
        entity.setSymbol(record.symbol());
        entity.setVenueOrderId(record.venueOrderId());
        entity.setVenueFillId(record.venueFillId());
        entity.setSide(record.side().name());
        entity.setQuantity(record.quantity());
        entity.setPrice(record.price());
        entity.setFee(record.fee());
        entity.setFeeAsset(record.feeAsset());
        entity.setRefId(record.refId());
        entity.setLedgerRefId(record.ledgerRefId());
        entity.setFilledAt(record.filledAt());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    public HedgeFillRecord toRecord() {
        return new HedgeFillRecord(
                UUID.fromString(id),
                marketMakerId,
                symbol,
                venueOrderId,
                venueFillId,
                OrderSide.valueOf(side),
                quantity,
                price,
                fee,
                feeAsset,
                refId,
                ledgerRefId,
                filledAt,
                createdAt
        );
    }
}
