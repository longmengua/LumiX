/*
 * 檔案用途：領域事件，描述交易、快照、資金費或強平等已發生的業務事實。
 */
package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Symbol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 成交事件（可回放）
 *
 * - uid: 哪位使用者
 * - symbol: 哪個交易對
 * - qty: 成交數量（含方向，買為 +，賣為 -）
 * - price: 成交價
 * - seq: 事件序號（由 EventStore 實作分配，用於去重/回放定位）
 * - ts: 事件時間
 *
 * 使用 Java 21 record 建模不可變事件。
 */
public record TradeExecuted(
        long uid,
        Symbol symbol,
        BigDecimal qty,
        BigDecimal price,
        long seq,
        Instant ts,
        UUID orderId,
        UUID counterOrderId,
        String matchId,
        boolean maker
) {
    public TradeExecuted(
            long uid,
            Symbol symbol,
            BigDecimal qty,
            BigDecimal price,
            long seq,
            Instant ts
    ) {
        this(uid, symbol, qty, price, seq, ts, null, null, null, false);
    }

    /** 產生帶新序號的事件（EventStore 追加時常會需要） */
    public TradeExecuted withSeq(long newSeq) {
        return new TradeExecuted(uid, symbol, qty, price, newSeq, ts, orderId, counterOrderId, matchId, maker);
        // 若想同時更新 ts，可視情況改為 new TradeExecuted(uid, symbol, qty, price, newSeq, Instant.now())
    }

    public BigDecimal absQty() {
        return qty == null ? BigDecimal.ZERO : qty.abs();
    }

    public BigDecimal notional() {
        if (price == null) return BigDecimal.ZERO;
        return price.multiply(absQty());
    }

    public String idempotencyKey() {
        String stableMatchId = matchId == null || matchId.isBlank() ? "seq-" + seq : matchId;
        String stableOrderId = orderId == null ? "unknown-order" : orderId.toString();
        return "trade-accounting:" + stableMatchId + ":" + stableOrderId;
    }
}
