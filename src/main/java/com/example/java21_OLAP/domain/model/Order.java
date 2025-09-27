package com.example.java21_OLAP.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order (訂單聚合)
 *
 * 重點：
 * - 使用 @Jacksonized 讓 Jackson 能用 Builder 反序列化（解決「no Creators」錯誤）
 * - 使用 @Builder.Default 為 id / status / ctime 提供預設值
 * - 將欄位改為「非 final」，避免反序列化卡住
 * - qty = 「剩餘數量」，撮合時會遞減；當 qty == 0 → status = FILLED
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    /** 訂單狀態 */
    public enum Status { NEW, PARTIALLY_FILLED, FILLED, CANCELED }

    /** 訂單唯一 ID（預設自動產生） */
    @Builder.Default
    private UUID id = UUID.randomUUID();

    /** 使用者 ID */
    private long uid;

    /** 交易對 */
    private Symbol symbol;

    /** 買/賣 */
    private OrderSide side;

    /** 訂單型別（LIMIT / MARKET） */
    private OrderType type;

    /** 價格（市價可為 null；本專案 MARKET 以極端價模擬） */
    private BigDecimal price;

    /** 剩餘數量（撮合成交會遞減；0 代表已完全成交） */
    private BigDecimal qty;

    /** 訂單狀態（預設 NEW） */
    @Builder.Default
    private Status status = Status.NEW;

    /** 建立時間（預設 now） */
    @Builder.Default
    private Instant ctime = Instant.now();

    /**
     * 訂單被成交（部分或全部）
     * @param execQty 本次成交量（正數）
     */
    public void fill(BigDecimal execQty) {
        if (execQty == null || execQty.signum() <= 0) return;

        this.qty = this.qty.subtract(execQty);
        if (this.qty.signum() <= 0) {
            this.qty = BigDecimal.ZERO;
            this.status = Status.FILLED;
        } else {
            this.status = Status.PARTIALLY_FILLED;
        }
    }
}
