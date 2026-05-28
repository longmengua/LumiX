/*
 * 檔案用途：測試 matching command/event log 的 owner epoch audit 欄位。
 */
package com.example.exchange.infra.matching;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.MatchingCommandLogEntry;
import com.example.exchange.domain.model.dto.MatchingEventLogEntry;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.MatchingCommandType;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matching log owner epoch tests。
 *
 * <p>此測試固定 worker fencing audit 欄位，確保未來 production command pipeline
 * 帶 ownerId / epoch 寫入時，log entry 能保留足夠資訊供追查 stale owner 問題。</p>
 */
class MatchingLogOwnerEpochTest {

    private final Symbol symbol = Symbol.builder()
            .base("BTC")
            .quote("USDT")
            .priceScale(2)
            .qtyScale(3)
            .build();

    @Test
    @DisplayName("command log fenced append 會保存 owner id 與 epoch")
    /**
     * 流程：用帶 owner 的 append 寫入 SUBMIT command -> 驗證 command entry 保留 ownerId 與 ownerEpoch。
     */
    void commandLogAppendStoresOwnerEpoch() {
        InMemoryMatchingCommandLog log = new InMemoryMatchingCommandLog();

        MatchingCommandLogEntry entry = log.append(
                "btcusdt",
                MatchingCommandType.SUBMIT,
                limit(1, OrderSide.BUY, "100", "1"),
                null,
                null,
                "worker-a",
                7L
        );

        assertThat(entry.ownerId()).isEqualTo("worker-a");
        assertThat(entry.ownerEpoch()).isEqualTo(7L);
        assertThat(log.listAll("BTCUSDT")).singleElement()
                .extracting(MatchingCommandLogEntry::ownerEpoch)
                .isEqualTo(7L);
    }

    @Test
    @DisplayName("event log fenced append 會保存 owner id 與 epoch")
    /**
     * 流程：用帶 owner 的 append 寫入成交 event -> 驗證 event entry 保留 ownerId 與 ownerEpoch。
     */
    void eventLogAppendStoresOwnerEpoch() {
        InMemoryMatchingEventLog log = new InMemoryMatchingEventLog();
        TradeExecuted trade = new TradeExecuted(
                1L,
                symbol,
                new BigDecimal("1"),
                new BigDecimal("100"),
                0L,
                Instant.now()
        );

        MatchingEventLogEntry entry = log.append("BTCUSDT", 3L, trade, "worker-a", 7L);

        assertThat(entry.ownerId()).isEqualTo("worker-a");
        assertThat(entry.ownerEpoch()).isEqualTo(7L);
        assertThat(log.listAll("BTCUSDT")).singleElement()
                .extracting(MatchingEventLogEntry::ownerEpoch)
                .isEqualTo(7L);
    }

    /**
     * 建立最小 LIMIT 測試訂單，專門用於 command log payload。
     */
    private Order limit(long uid, OrderSide side, String price, String qty) {
        return Order.builder()
                .uid(uid)
                .symbol(symbol)
                .side(side)
                .type(OrderType.LIMIT)
                .price(new BigDecimal(price))
                .qty(new BigDecimal(qty))
                .origQty(new BigDecimal(qty))
                .build();
    }
}
