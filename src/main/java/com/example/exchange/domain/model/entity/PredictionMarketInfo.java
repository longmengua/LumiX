package com.example.exchange.domain.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Polymarket outcome market 資訊。
 *
 * 這張表是 outcome 層資料。
 *
 * 一場比賽通常會有三筆：
 * - homeWin
 * - draw
 * - awayWin
 *
 * MVP 階段：
 * metadata + price 都先存在這張表。
 */
@Getter
@Setter
@Entity
@Table(
        name = "prediction_market_info",
        indexes = {
                @Index(name = "idx_pm_info_event_slug", columnList = "event_slug"),
                @Index(name = "idx_pm_info_outcome", columnList = "outcome_key"),
                @Index(name = "idx_pm_info_price_updated", columnList = "last_price_updated_at"),
                @Index(name = "idx_pm_info_active_closed", columnList = "active, closed")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pm_market_slug",
                        columnNames = "market_slug"
                )
        }
)
public class PredictionMarketInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * event 層 slug。
     */
    @Column(name = "event_slug", nullable = false, length = 128)
    private String eventSlug;

    @Column(name = "event_title", length = 256)
    private String eventTitle;

    @Column(name = "team_a", length = 128)
    private String teamA;

    @Column(name = "team_b", length = 128)
    private String teamB;

    @Column(name = "event_date")
    private LocalDate eventDate;

    /**
     * Polymarket condition id。
     */
    @Column(name = "condition_id", length = 128)
    private String conditionId;

    /**
     * Polymarket question。
     */
    @Column(name = "question", length = 512)
    private String question;

    /**
     * outcome market slug。
     */
    @Column(name = "market_slug", nullable = false, length = 256)
    private String marketSlug;

    /**
     * homeWin / draw / awayWin
     */
    @Column(name = "outcome_key", nullable = false, length = 32)
    private String outcomeKey;

    /**
     * Mexico / Draw / South Africa
     */
    @Column(name = "outcome_label", length = 128)
    private String outcomeLabel;

    /**
     * YES token id。
     */
    @Column(name = "yes_token_id", length = 256)
    private String yesTokenId;

    /**
     * NO token id。
     */
    @Column(name = "no_token_id", length = 256)
    private String noTokenId;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "closed")
    private Boolean closed;

    @Column(name = "accepting_orders")
    private Boolean acceptingOrders;

    @Column(name = "enable_order_book")
    private Boolean enableOrderBook;

    /**
     * Gamma bestBid。
     *
     * 前端 sellPrice 優先用這個。
     */
    @Column(name = "best_bid")
    private Double bestBid;

    /**
     * Gamma bestAsk。
     *
     * 前端 buyPrice 優先用這個。
     */
    @Column(name = "best_ask")
    private Double bestAsk;

    @Column(name = "last_trade_price")
    private Double lastTradePrice;

    /**
     * outcomePrices[0]。
     */
    @Column(name = "static_yes_price")
    private Double staticYesPrice;

    /**
     * outcomePrices[1]。
     */
    @Column(name = "static_no_price")
    private Double staticNoPrice;

    @Column(name = "liquidity")
    private Double liquidity;

    @Column(name = "volume")
    private Double volume;

    @Column(name = "volume_24hr")
    private Double volume24hr;

    /**
     * Gamma outcomePrices 原始值。
     */
    @Lob
    @Column(name = "outcome_prices", columnDefinition = "TEXT")
    private String outcomePrices;

    /**
     * Gamma clobTokenIds 原始值。
     */
    @Lob
    @Column(name = "clob_token_ids", columnDefinition = "TEXT")
    private String clobTokenIds;

    /**
     * 最後價格更新時間。
     *
     * 每 5 秒刷新時會判斷這個欄位。
     */
    @Column(name = "last_price_updated_at")
    private LocalDateTime lastPriceUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (lastPriceUpdatedAt == null) {
            lastPriceUpdatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }


}