/*
 * 檔案用途：JPA entity，保存做市商最新 quote active-state 與 bid/ask order ownership。
 */
package com.example.exchange.domain.model.entity;

import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "market_maker_quote_states",
        indexes = {
                @Index(name = "idx_mm_quote_states_mm_updated", columnList = "market_maker_id,updated_at"),
                @Index(name = "idx_mm_quote_states_active_updated", columnList = "active,updated_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mm_quote_states_mm_symbol",
                        columnNames = {"market_maker_id", "symbol"}
                )
        }
)
public class MarketMakerQuoteStateRecord {

    @Id
    @Column(name = "id", nullable = false, length = 192)
    private String id;

    @Column(name = "market_maker_id", nullable = false, length = 128)
    private String marketMakerId;

    @Column(name = "uid", nullable = false)
    private Long uid;

    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    @Column(name = "ref_id", length = 128)
    private String refId;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "accepted", nullable = false)
    private Boolean accepted;

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "canceled_count", nullable = false)
    private Integer canceledCount;

    @Column(name = "bid_order_id", length = 36)
    private String bidOrderId;

    @Column(name = "ask_order_id", length = 36)
    private String askOrderId;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public static MarketMakerQuoteStateRecord from(MarketMakerQuoteState state) {
        MarketMakerQuoteStateRecord record = new MarketMakerQuoteStateRecord();
        record.setId(id(state.marketMakerId(), state.symbol()));
        record.setMarketMakerId(state.marketMakerId());
        record.setUid(state.uid());
        record.setSymbol(state.symbol());
        record.setRefId(state.refId());
        record.setActive(state.active());
        record.setAccepted(state.accepted());
        record.setReason(state.reason());
        record.setCanceledCount(state.canceledCount());
        record.setBidOrderId(state.bidOrderId() == null ? null : state.bidOrderId().toString());
        record.setAskOrderId(state.askOrderId() == null ? null : state.askOrderId().toString());
        record.setUpdatedAt(state.updatedAt());
        return record;
    }

    public MarketMakerQuoteState toState() {
        return new MarketMakerQuoteState(
                marketMakerId,
                uid,
                symbol,
                refId,
                Boolean.TRUE.equals(active),
                Boolean.TRUE.equals(accepted),
                reason,
                canceledCount == null ? 0 : canceledCount,
                bidOrderId == null ? null : UUID.fromString(bidOrderId),
                askOrderId == null ? null : UUID.fromString(askOrderId),
                updatedAt
        );
    }

    public static String id(String marketMakerId, String symbol) {
        return marketMakerId.trim() + ":" + symbol.trim().toUpperCase();
    }
}
