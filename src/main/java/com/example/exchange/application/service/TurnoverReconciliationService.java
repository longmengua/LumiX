/*
 * 檔案用途：應用服務，對帳 turnover read model 與 trade tape。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverReconciliationIssue;
import com.example.exchange.domain.model.dto.TurnoverReconciliationReport;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import com.example.exchange.domain.repository.TurnoverStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TurnoverReconciliationService {

    private final TurnoverStore turnoverStore;
    private final MarketDataTradeTapeStore tradeTapeStore;

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
                turnover.quantity(),
                tapeQuantity,
                turnover.notional(),
                tapeNotional
        );
    }
}
