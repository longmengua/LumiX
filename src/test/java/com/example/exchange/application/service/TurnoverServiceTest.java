/*
 * 檔案用途：測試 TurnoverService 由成交事件建立 turnover read model。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverSummary;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.TurnoverStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TurnoverServiceTest {

    @Test
    @DisplayName("recordTrade 會保存 uid/account/symbol/strategy/match 維度與 notional")
    void recordTradeStoresAuditableTurnoverFact() {
        MemTurnoverStore store = new MemTurnoverStore();
        TurnoverService service = new TurnoverService(store);
        UUID orderId = UUID.randomUUID();
        Symbol symbol = Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build();
        Order order = Order.builder()
                .id(orderId)
                .uid(41)
                .symbol(symbol)
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .clientOrderId("strategy-a")
                .build();
        TradeExecuted trade = new TradeExecuted(
                41,
                symbol,
                new BigDecimal("-2.500"),
                new BigDecimal("100.00"),
                77,
                Instant.parse("2026-05-27T01:02:03Z"),
                orderId,
                UUID.randomUUID(),
                "match-77",
                false
        );

        // 流程：成交事件進入 turnover service，read model 用絕對數量與 notional 對帳。
        service.recordTrade(trade, order);
        TurnoverSummary summary = service.summarizeUser(41);

        assertThat(store.records).hasSize(1);
        TurnoverRecord record = store.records.getFirst();
        assertThat(record.uid()).isEqualTo(41);
        assertThat(record.accountId()).isEqualTo("41");
        assertThat(record.symbol()).isEqualTo("BTCUSDT");
        assertThat(record.strategyId()).isEqualTo("strategy-a");
        assertThat(record.matchId()).isEqualTo("match-77");
        assertThat(record.quantity()).isEqualByComparingTo("2.500");
        assertThat(record.notional()).isEqualByComparingTo("250.00000");
        assertThat(summary.tradeCount()).isEqualTo(1);
        assertThat(summary.notional()).isEqualByComparingTo("250.00000");
    }

    @Test
    @DisplayName("summarizeMatch 只統計指定 matchId 的 turnover")
    void summarizeMatchFiltersByMatchId() {
        MemTurnoverStore store = new MemTurnoverStore();
        TurnoverService service = new TurnoverService(store);
        UUID orderId = UUID.randomUUID();
        Symbol symbol = Symbol.builder().base("ETH").quote("USDT").priceScale(2).qtyScale(3).build();

        // 流程：同一使用者兩筆不同 match，只應把指定 match 納入對帳 summary。
        service.recordTrade(trade(51, symbol, orderId, "match-a", "10"), null);
        service.recordTrade(trade(51, symbol, UUID.randomUUID(), "match-b", "20"), null);

        TurnoverSummary summary = service.summarizeMatch(51, "match-a");

        assertThat(summary.tradeCount()).isEqualTo(1);
        assertThat(summary.notional()).isEqualByComparingTo("10.000");
    }

    private static TradeExecuted trade(long uid, Symbol symbol, UUID orderId, String matchId, String price) {
        return new TradeExecuted(
                uid,
                symbol,
                BigDecimal.ONE,
                new BigDecimal(price),
                Math.abs(matchId.hashCode()),
                Instant.parse("2026-05-27T01:02:03Z"),
                orderId,
                UUID.randomUUID(),
                matchId,
                false
        );
    }

    private static class MemTurnoverStore implements TurnoverStore {
        private final List<TurnoverRecord> records = new ArrayList<>();

        @Override
        public void append(TurnoverRecord record) {
            records.add(record);
        }

        @Override
        public List<TurnoverRecord> findByUid(long uid) {
            return records.stream()
                    .filter(record -> record.uid() == uid)
                    .toList();
        }

        @Override
        public List<TurnoverRecord> findByMatchId(String matchId) {
            return records.stream()
                    .filter(record -> matchId.equals(record.matchId()))
                    .toList();
        }
    }
}
