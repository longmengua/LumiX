package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Symbol;

import java.math.BigDecimal;
import java.time.Instant;

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
        Instant ts
) {
    /** 產生帶新序號的事件（EventStore 追加時常會需要） */
    public TradeExecuted withSeq(long newSeq) {
        return new TradeExecuted(uid, symbol, qty, price, newSeq, ts);
        // 若想同時更新 ts，可視情況改為 new TradeExecuted(uid, symbol, qty, price, newSeq, Instant.now())
    }
}
