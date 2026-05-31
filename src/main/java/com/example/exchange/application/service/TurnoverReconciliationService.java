/*
 * 檔案用途：應用服務，對帳 turnover read model 與 trade tape。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverReconciliationBatchReport;
import com.example.exchange.domain.model.dto.TurnoverReconciliationIssue;
import com.example.exchange.domain.model.dto.TurnoverReconciliationReport;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.domain.repository.TurnoverStore;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TurnoverReconciliationService {

    private final TurnoverStore turnoverStore;
    private final MarketDataTradeTapeStore tradeTapeStore;
    private WalletLedgerJournal ledgerJournal;

    @Autowired(required = false)
    public void setLedgerJournal(WalletLedgerJournal ledgerJournal) {
        this.ledgerJournal = ledgerJournal;
    }

    public TurnoverReconciliationReport reconcileMatch(long uid, String matchId) {
        if (matchId == null || matchId.isBlank()) {
            return new TurnoverReconciliationReport(
                    uid,
                    null,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    Instant.now(),
                    List.of()
            );
        }
        String normalizedMatchId = matchId.trim();
        List<TurnoverRecord> turnoverRecords = turnoverStore.findByMatchId(normalizedMatchId)
                .stream()
                .filter(record -> record.uid() == uid)
                .toList();
        List<TradeTapeItem> tradeTapeItems = tradeTapeStore.findByMatchId(normalizedMatchId);
        List<TurnoverReconciliationIssue> issues = new ArrayList<>();
        for (TurnoverRecord turnover : turnoverRecords) {
            TradeTapeItem tape = tradeTapeItems.stream()
                    .filter(item -> Objects.equals(item.orderId(), turnover.orderId()))
                    .findFirst()
                    .orElse(null);
            if (tape == null) {
                issues.add(issue(
                        "TURNOVER_TRADE_TAPE_MISSING",
                        "turnover record has no matching trade tape item",
                        turnover,
                        null
                ));
                continue;
            }
            if (turnover.price().compareTo(tape.price()) != 0
                    || turnover.quantity().compareTo(tape.qty().abs()) != 0
                    || turnover.notional().compareTo(tape.price().multiply(tape.qty().abs())) != 0) {
                issues.add(issue(
                        "TURNOVER_TRADE_TAPE_MISMATCH",
                        "turnover record differs from trade tape price, quantity, or notional",
                        turnover,
                        tape
                ));
            }
        }
        if (!turnoverRecords.isEmpty()
                && ledgerJournal != null
                && ledgerJournal.findByRefId(normalizedMatchId).isEmpty()) {
            TurnoverRecord first = turnoverRecords.getFirst();
            issues.add(new TurnoverReconciliationIssue(
                    "TURNOVER_LEDGER_REF_MISSING",
                    "turnover match has no durable ledger journal entries with the same refId",
                    uid,
                    normalizedMatchId,
                    first.orderId(),
                    first.strategyId(),
                    first.marketMakerId(),
                    false,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    turnoverRecords.stream()
                            .map(TurnoverRecord::notional)
                            .reduce(BigDecimal.ZERO, BigDecimal::add),
                    BigDecimal.ZERO
            ));
        }
        BigDecimal turnoverNotional = turnoverRecords.stream()
                .map(TurnoverRecord::notional)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tapeNotional = tradeTapeItems.stream()
                .map(item -> item.price().multiply(item.qty().abs()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TurnoverReconciliationReport(
                uid,
                normalizedMatchId,
                turnoverRecords.size(),
                tradeTapeItems.size(),
                turnoverNotional,
                tapeNotional,
                issues.size(),
                Instant.now(),
                issues
        );
    }

    public TurnoverReconciliationBatchReport reconcileRecent(Instant fromInclusive, Instant toExclusive, int limit) {
        Instant upper = toExclusive == null ? Instant.now() : toExclusive;
        Instant lower = fromInclusive == null ? upper.minusSeconds(3_600) : fromInclusive;
        if (!lower.isBefore(upper)) {
            return new TurnoverReconciliationBatchReport(lower, upper, 0, 0, 0, Instant.now(), List.of());
        }
        int boundedLimit = Math.max(1, Math.min(limit, 10_000));
        List<TurnoverRecord> sampled = turnoverStore.findByCreatedAtBetween(lower, upper, boundedLimit);
        Map<String, TurnoverRecord> firstByUidMatch = new LinkedHashMap<>();
        for (TurnoverRecord record : sampled) {
            if (record.matchId() == null || record.matchId().isBlank()) {
                continue;
            }
            firstByUidMatch.putIfAbsent(record.uid() + "|" + record.matchId().trim(), record);
        }
        List<TurnoverReconciliationReport> reports = new ArrayList<>();
        int issues = 0;
        for (TurnoverRecord record : firstByUidMatch.values()) {
            TurnoverReconciliationReport report = reconcileMatch(record.uid(), record.matchId());
            reports.add(report);
            issues += report.issueCount();
        }
        return new TurnoverReconciliationBatchReport(
                lower,
                upper,
                sampled.size(),
                reports.size(),
                issues,
                Instant.now(),
                reports
        );
    }

    private static TurnoverReconciliationIssue issue(
            String code,
            String message,
            TurnoverRecord turnover,
            TradeTapeItem tape
    ) {
        BigDecimal tapeQuantity = tape == null ? BigDecimal.ZERO : tape.qty().abs();
        BigDecimal tapeNotional = tape == null ? BigDecimal.ZERO : tape.price().multiply(tape.qty().abs());
        return new TurnoverReconciliationIssue(
                code,
                message,
                turnover.uid(),
                turnover.matchId(),
                turnover.orderId(),
                turnover.strategyId(),
                turnover.marketMakerId(),
                true,
                turnover.quantity(),
                tapeQuantity,
                turnover.notional(),
                tapeNotional
        );
    }
}
