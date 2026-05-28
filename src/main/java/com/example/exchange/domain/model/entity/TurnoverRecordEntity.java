/*
 * 檔案用途：JPA entity，保存 turnover read model。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.TurnoverRecord;
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
        name = "turnover_records",
        indexes = {
                @Index(name = "idx_turnover_uid_created", columnList = "uid,created_at"),
                @Index(name = "idx_turnover_symbol_created", columnList = "symbol,created_at"),
                @Index(name = "idx_turnover_strategy", columnList = "uid,strategy_id,created_at"),
                @Index(name = "idx_turnover_market_maker", columnList = "market_maker_id,created_at"),
                @Index(name = "idx_turnover_match", columnList = "match_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_turnover_trade_order",
                        columnNames = {"trade_seq", "order_id"}
                )
        }
)
public class TurnoverRecordEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "account_id", length = 64)
    private String accountId;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "strategy_id", length = 128)
    private String strategyId;

    @Column(name = "market_maker_id", length = 128)
    private String marketMakerId;

    @Column(name = "order_id", nullable = false, length = 36)
    private String orderId;

    @Column(name = "match_id", length = 128)
    private String matchId;

    @Column(name = "trade_seq", nullable = false)
    private Long tradeSeq;

    @Column(name = "quantity", nullable = false, precision = 38, scale = 18)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "notional", nullable = false, precision = 38, scale = 18)
    private BigDecimal notional;

    @Column(name = "traded_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant tradedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    public static TurnoverRecordEntity from(TurnoverRecord record, int schemaVersion) {
        TurnoverRecordEntity entity = new TurnoverRecordEntity();
        entity.setId(record.id().toString());
        entity.setSchemaVersion(schemaVersion);
        entity.setUid(record.uid());
        entity.setAccountId(record.accountId());
        entity.setSymbol(record.symbol());
        entity.setStrategyId(record.strategyId());
        entity.setMarketMakerId(record.marketMakerId());
        entity.setOrderId(record.orderId() == null ? null : record.orderId().toString());
        entity.setMatchId(record.matchId());
        entity.setTradeSeq(record.tradeSeq());
        entity.setQuantity(record.quantity());
        entity.setPrice(record.price());
        entity.setNotional(record.notional());
        entity.setTradedAt(record.tradedAt());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }

    public TurnoverRecord toRecord() {
        return new TurnoverRecord(
                UUID.fromString(id),
                uid,
                accountId,
                symbol,
                strategyId,
                marketMakerId,
                UUID.fromString(orderId),
                matchId,
                tradeSeq,
                quantity,
                price,
                notional,
                tradedAt,
                createdAt
        );
    }
}
