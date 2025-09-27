package com.example.exchange.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.math.RoundingMode;

/**
 * Position (持倉)
 *
 * - 每個 user 對某個 symbol 有一個倉位
 * - qty > 0 表示做多；qty < 0 表示做空
 * - entryPrice：進場均價（按倉位變動加權更新）
 * - margin：Isolated 模式下的隔離保證金（CROSS 模式不使用）
 *
 * 重點：
 * - 使用 @Jacksonized 讓 Jackson 能用 Builder 反序列化（解決 no Creators）
 * - 使用 @Builder.Default 提供初始值
 * - 欄位不再是 final，利於反序列化
 */
@Data
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Position {

    /** 使用者 ID */
    private long uid;

    /** 交易對 */
    private Symbol symbol;

    /** 保證金模式（CROSS / ISOLATED） */
    private MarginMode mode;

    /** 槓桿倍數 */
    private BigDecimal leverage;

    /** 持倉數量（帶正負號；>0 多，<0 空） */
    @Builder.Default
    private BigDecimal qty = BigDecimal.ZERO;

    /** 進場均價（當前持倉的加權平均價；qty=0 時設為 0） */
    @Builder.Default
    private BigDecimal entryPrice = BigDecimal.ZERO;

    /** 隔離保證金（CROSS 模式不使用） */
    @Builder.Default
    private BigDecimal margin = BigDecimal.ZERO;

    /** 最近一次更新時間 */
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * 套用成交 → 更新持倉均價與數量
     * @param tradeQty   本次成交數量（帶正負號；+買入/-賣出）
     * @param tradePrice 本次成交價格（正數）
     */
    public void applyTrade(BigDecimal tradeQty, BigDecimal tradePrice) {
        if (tradeQty == null || tradePrice == null) return;
        if (tradeQty.signum() == 0) return;

        // 舊倉名義金額（用絕對數量）
        BigDecimal notionalOld = safe(entryPrice).multiply(abs(qty));
        BigDecimal notionalNew = safe(tradePrice).multiply(abs(tradeQty));
        BigDecimal newQty = safe(qty).add(tradeQty);

        if (newQty.signum() == 0) {
            // 倉位被完全平掉
            qty = BigDecimal.ZERO;
            entryPrice = BigDecimal.ZERO;
        } else if (qty.signum() == 0 || qty.signum() == tradeQty.signum()) {
            // 新增倉位或加倉：重新計算加權均價
            BigDecimal denom = abs(qty).add(abs(tradeQty));
            int scale = (symbol != null) ? symbol.getPriceScale() : 8; // 防守性：symbol 可能為 null
            entryPrice = notionalOld.add(notionalNew)
                    .divide(denom, scale, RoundingMode.HALF_UP);
            qty = newQty;
        } else {
            // 減倉：僅改變倉位數量；若完全平倉則均價歸零
            qty = newQty;
            if (qty.signum() == 0) entryPrice = BigDecimal.ZERO;
        }

        updatedAt = Instant.now();
    }

    // ---- 小工具 ----
    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal abs(BigDecimal v) { return safe(v).abs(); }
}
