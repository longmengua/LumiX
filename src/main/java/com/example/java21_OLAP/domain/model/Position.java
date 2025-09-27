package com.example.java21_OLAP.domain.model;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Position (持倉)
 *
 * - 每個 user 對某個 symbol 有一個倉位
 * - qty > 0 表示做多；qty < 0 表示做空
 * - entryPrice 記錄進場均價
 * - margin: Isolated 模式下才會有獨立 margin
 */
@Data
public class Position {

    private final long uid;
    private final Symbol symbol;
    private final MarginMode mode;
    private BigDecimal leverage;  // 槓桿倍數
    private BigDecimal qty;       // 持倉數量 (signed)
    private BigDecimal entryPrice; // 均價
    private BigDecimal margin;     // 隔離保證金 (CROSS 模式時不使用)
    private Instant updatedAt;

    public Position(long uid, Symbol symbol, MarginMode mode, BigDecimal leverage) {
        this.uid = uid;
        this.symbol = symbol;
        this.mode = mode;
        this.leverage = leverage;
        this.qty = BigDecimal.ZERO;
        this.entryPrice = BigDecimal.ZERO;
        this.margin = BigDecimal.ZERO;
        this.updatedAt = Instant.now();
    }

    /** 套用成交 → 更新持倉均價與數量 */
    public void applyTrade(BigDecimal tradeQty, BigDecimal tradePrice) {
        BigDecimal notionalOld = entryPrice.multiply(qty.abs());
        BigDecimal notionalNew = tradePrice.multiply(tradeQty.abs());
        BigDecimal newQty = qty.add(tradeQty);

        if (newQty.signum() == 0) {
            // 倉位被完全平掉
            qty = BigDecimal.ZERO;
            entryPrice = BigDecimal.ZERO;
        } else if (qty.signum() == 0 || qty.signum() == tradeQty.signum()) {
            // 新增倉位或加倉
            entryPrice = notionalOld.add(notionalNew)
                    .divide(qty.abs().add(tradeQty.abs()),
                            symbol.priceScale(),
                            java.math.RoundingMode.HALF_UP);
            qty = newQty;
        } else {
            // 減倉，不影響 entryPrice
            qty = newQty;
            if (qty.signum() == 0) entryPrice = BigDecimal.ZERO;
        }
        updatedAt = Instant.now();
    }
}
