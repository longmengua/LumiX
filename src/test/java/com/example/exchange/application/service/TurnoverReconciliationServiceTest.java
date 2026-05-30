/*
 * 檔案用途：測試 turnover read model 與 trade tape 對帳。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverReconciliationReport;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.domain.repository.TurnoverStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TurnoverReconciliationServiceTest {

    @Test
    @DisplayName("reconcileMatch 比對 turnover 與 trade tape 的 match/order/price/qty/notional")
    void reconcileMatchComparesTurnoverRecordsWithTradeTape() {
        MemTurnoverStore turnoverStore = new MemTurnoverStore();
        MemTradeTapeStore tradeTapeStore = new MemTradeTapeStore();
        TurnoverReconciliationService service = new TurnoverReconciliationService(turnoverStore, tradeTapeStore);
        UUID matchedOrder = UUID.randomUUID();
        UUID missingOrder = UUID.randomUUID();
        turnoverStore.append(record(81, matchedOrder, "match-a", "2", "100"));
        turnoverStore.append(record(81, missingOrder, "match-a", "1", "50"));
        tradeTapeStore.append(item(matchedOrder, "match-a", "2", "100"));

        // 場景：一筆 turnover 能對上 trade tape，另一筆缺 tape，報告要明確指出缺口。
        TurnoverReconciliationReport report = service.reconcileMatch(81, "match-a");

        assertThat(report.turnoverRecordCount()).isEqualTo(2);
        assertThat(report.tradeTapeRecordCount()).isEqualTo(1);
        assertThat(report.turnoverNotional()).isEqualByComparingTo("250");
        assertThat(report.tradeTapeNotional()).isEqualByComparingTo("200");
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().code()).isEqualTo("TURNOVER_TRADE_TAPE_MISSING");
        assertThat(report.issues().getFirst().orderId()).isEqualTo(missingOrder);
    }

    @Test
    @DisplayName("reconcileMatch 會回報 turnover 與 trade tape 數量或 notional 不一致")
    void reconcileMatchReportsValueMismatch() {
        MemTurnoverStore turnoverStore = new MemTurnoverStore();
        MemTradeTapeStore tradeTapeStore = new MemTradeTapeStore();
        TurnoverReconciliationService service = new TurnoverReconciliationService(turnoverStore, tradeTapeStore);
        UUID orderId = UUID.randomUUID();
        turnoverStore.append(record(82, orderId, "match-b", "2", "100"));
        tradeTapeStore.append(item(orderId, "match-b", "3", "100"));

        // 場景：trade tape 有同 order/match，但 qty 不一致，要列成 mismatch issue。
        TurnoverReconciliationReport report = service.reconcileMatch(82, "match-b");

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().code()).isEqualTo("TURNOVER_TRADE_TAPE_MISMATCH");
        assertThat(report.issues().getFirst().turnoverQuantity()).isEqualByComparingTo("2");
        assertThat(report.issues().getFirst().tradeTapeQuantity()).isEqualByComparingTo("3");
    }

    private static TurnoverRecord record(long uid, UUID orderId, String matchId, String qty, String price) {
        BigDecimal quantity = new BigDecimal(qty);
        BigDecimal tradePrice = new BigDecimal(price);
        return new TurnoverRecord(
                UUID.randomUUID(),
                uid,
                String.valueOf(uid),
                "BTCUSDT",
                "strategy-a",
                "mm-1",
                orderId,
                matchId,
                Math.abs(orderId.hashCode()),
                quantity,
                tradePrice,
                quantity.multiply(tradePrice),
                Instant.parse("2026-05-30T00:00:00Z"),
                Instant.parse("2026-05-30T00:00:01Z")
        );
    }

    private static TradeTapeItem item(UUID orderId, String matchId, String qty, String price) {
        return new TradeTapeItem(
                "BTCUSDT",
                matchId,
                orderId,
                OrderSide.BUY,
                new BigDecimal(price),
                new BigDecimal(qty),
                false,
                Instant.parse("2026-05-30T00:00:00Z")
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

    private static class MemTradeTapeStore implements MarketDataTradeTapeStore {
        private final List<TradeTapeItem> items = new ArrayList<>();

        @Override
        public void append(TradeTapeItem item) {
            items.add(item);
        }

        @Override
        public List<TradeTapeItem> findRecent(String symbol, int limit) {
            return items.stream()
                    .filter(item -> symbol.equals(item.symbol()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<TradeTapeItem> findByMatchId(String matchId) {
            return items.stream()
                    .filter(item -> matchId.equals(item.matchId()))
                    .toList();
        }
    }
}
