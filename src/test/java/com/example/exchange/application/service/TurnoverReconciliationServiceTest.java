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
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
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

    @Test
    @DisplayName("reconcileMatch 會回報成交 match 缺少同 refId ledger journal")
    void reconcileMatchReportsMissingLedgerRefWhenJournalIsAvailable() {
        MemTurnoverStore turnoverStore = new MemTurnoverStore();
        MemTradeTapeStore tradeTapeStore = new MemTradeTapeStore();
        TurnoverReconciliationService service = new TurnoverReconciliationService(turnoverStore, tradeTapeStore);
        service.setLedgerJournal(new EmptyLedgerJournal());
        UUID orderId = UUID.randomUUID();
        turnoverStore.append(record(83, orderId, "match-ledger", "2", "100"));
        tradeTapeStore.append(item(orderId, "match-ledger", "2", "100"));

        // 場景：trade tape 對得上，但 ledger refId 沒有任何 durable journal，應列為帳務追蹤缺口。
        TurnoverReconciliationReport report = service.reconcileMatch(83, "match-ledger");

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().code()).isEqualTo("TURNOVER_LEDGER_REF_MISSING");
        assertThat(report.issues().getFirst().turnoverNotional()).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("reconcileRecent 依 createdAt window 抽樣並彙總 uid+match 對帳結果")
    void reconcileRecentAggregatesWindowByUidAndMatch() {
        MemTurnoverStore turnoverStore = new MemTurnoverStore();
        MemTradeTapeStore tradeTapeStore = new MemTradeTapeStore();
        TurnoverReconciliationService service = new TurnoverReconciliationService(turnoverStore, tradeTapeStore);
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();
        turnoverStore.append(record(84, orderA, "match-window", "2", "100"));
        turnoverStore.append(record(84, orderB, "match-window", "1", "50"));
        tradeTapeStore.append(item(orderA, "match-window", "2", "100"));

        // 場景：batch 只產生一份 uid+match 報告，並彙總底下缺 tape 的 issue 數。
        var report = service.reconcileRecent(
                Instant.parse("2026-05-29T00:00:00Z"),
                Instant.parse("2026-05-31T00:00:00Z"),
                100
        );

        assertThat(report.sampledRecordCount()).isEqualTo(2);
        assertThat(report.matchReportCount()).isEqualTo(1);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.reports().getFirst().matchId()).isEqualTo("match-window");
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

        @Override
        public List<TurnoverRecord> findByCreatedAtBetween(Instant fromInclusive, Instant toExclusive, int limit) {
            return records.stream()
                    .filter(record -> !record.createdAt().isBefore(fromInclusive))
                    .filter(record -> record.createdAt().isBefore(toExclusive))
                    .limit(limit)
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

    private static class EmptyLedgerJournal implements WalletLedgerJournal {
        @Override
        public void append(WalletLedgerEntry entry) {
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return List.of();
        }

        @Override
        public List<WalletLedgerEntry> findByUidAndAsset(long uid, String asset) {
            return List.of();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return List.of();
        }
    }
}
