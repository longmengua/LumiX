/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.model.dto.PositionChange;
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

    /** 累積已實現損益 */
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    /** 累積已支付手續費 */
    @Builder.Default
    private BigDecimal feePaid = BigDecimal.ZERO;

    /** 累積已取得返佣 */
    @Builder.Default
    private BigDecimal rebateEarned = BigDecimal.ZERO;

    /** 累積已支付資金費 */
    @Builder.Default
    private BigDecimal fundingPaid = BigDecimal.ZERO;

    /** 累積已收取資金費 */
    @Builder.Default
    private BigDecimal fundingReceived = BigDecimal.ZERO;

    /** 累積由保險基金承接的清算缺口 */
    @Builder.Default
    private BigDecimal insuranceFundCovered = BigDecimal.ZERO;

    /** 累積進入 ADL/社會化損失的清算缺口 */
    @Builder.Default
    private BigDecimal adlCovered = BigDecimal.ZERO;

    /** 最近一次更新時間 */
    @Builder.Default
    private Instant updatedAt = Instant.now();

    /**
     * 套用成交 → 更新持倉均價與數量
     * @param tradeQty   本次成交數量（帶正負號；+買入/-賣出）
     * @param tradePrice 本次成交價格（正數）
     */
    public void applyTrade(BigDecimal tradeQty, BigDecimal tradePrice) {
        applyTradeWithPnl(tradeQty, tradePrice);
    }

    /**
     * 套用成交並回傳本次實現損益。
     */
    public PositionChange applyTradeWithPnl(BigDecimal tradeQty, BigDecimal tradePrice) {
        if (tradeQty == null || tradePrice == null) {
            return new PositionChange(qty, qty, entryPrice, entryPrice, BigDecimal.ZERO);
        }
        if (tradeQty.signum() == 0) {
            return new PositionChange(qty, qty, entryPrice, entryPrice, BigDecimal.ZERO);
        }

        BigDecimal oldQty = safe(qty);
        BigDecimal oldEntry = safe(entryPrice);
        BigDecimal realized = calculateRealizedPnl(oldQty, oldEntry, tradeQty, tradePrice);

        // 舊倉名義金額（用絕對數量）
        BigDecimal notionalOld = oldEntry.multiply(abs(oldQty));
        BigDecimal notionalNew = safe(tradePrice).multiply(abs(tradeQty));
        BigDecimal newQty = oldQty.add(tradeQty);

        if (newQty.signum() == 0) {
            // 倉位被完全平掉
            qty = BigDecimal.ZERO;
            entryPrice = BigDecimal.ZERO;
        } else if (oldQty.signum() == 0 || oldQty.signum() == tradeQty.signum()) {
            // 新增倉位或加倉：重新計算加權均價
            BigDecimal denom = abs(oldQty).add(abs(tradeQty));
            int scale = (symbol != null) ? symbol.getPriceScale() : 8; // 防守性：symbol 可能為 null
            entryPrice = notionalOld.add(notionalNew)
                    .divide(denom, scale, RoundingMode.HALF_UP);
            qty = newQty;
        } else {
            // 減倉或反手：減倉保留舊均價；反手後剩餘部位以本次成交價作為新均價。
            qty = newQty;
            if (oldQty.signum() != newQty.signum()) {
                entryPrice = tradePrice;
            } else if (qty.signum() == 0) {
                entryPrice = BigDecimal.ZERO;
            }
        }

        realizedPnl = safe(realizedPnl).add(realized);
        updatedAt = Instant.now();
        return new PositionChange(oldQty, qty, oldEntry, entryPrice, realized);
    }

    public void addFeePaid(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            feePaid = safe(feePaid).add(amount);
            updatedAt = Instant.now();
        }
    }

    public void addRebateEarned(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            rebateEarned = safe(rebateEarned).add(amount);
            updatedAt = Instant.now();
        }
    }

    public void addFunding(BigDecimal cashflow) {
        if (cashflow == null || cashflow.signum() == 0) return;
        if (cashflow.signum() > 0) {
            fundingReceived = safe(fundingReceived).add(cashflow);
        } else {
            fundingPaid = safe(fundingPaid).add(cashflow.abs());
        }
        updatedAt = Instant.now();
    }

    public void addInsuranceFundCovered(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            insuranceFundCovered = safe(insuranceFundCovered).add(amount);
            updatedAt = Instant.now();
        }
    }

    public void addAdlCovered(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            adlCovered = safe(adlCovered).add(amount);
            updatedAt = Instant.now();
        }
    }

    private static BigDecimal calculateRealizedPnl(
            BigDecimal oldQty,
            BigDecimal oldEntry,
            BigDecimal tradeQty,
            BigDecimal tradePrice
    ) {
        if (oldQty.signum() == 0 || oldQty.signum() == tradeQty.signum()) {
            return BigDecimal.ZERO;
        }

        BigDecimal closingQty = abs(oldQty).min(abs(tradeQty));
        if (oldQty.signum() > 0) {
            return tradePrice.subtract(oldEntry).multiply(closingQty);
        }
        return oldEntry.subtract(tradePrice).multiply(closingQty);
    }

    // ---- 小工具 ----
    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal abs(BigDecimal v) { return safe(v).abs(); }
}
