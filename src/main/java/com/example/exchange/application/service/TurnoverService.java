/*
 * 檔案用途：應用服務，將成交事件轉成可對帳的 turnover read model。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.TurnoverRecord;
import com.example.exchange.domain.model.dto.TurnoverSummary;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.repository.TurnoverStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TurnoverService {

    private final TurnoverStore turnoverStore;

    public void recordTrade(TradeExecuted trade, Order order) {
        if (trade == null || trade.orderId() == null || trade.symbol() == null) {
            return;
        }
        // clientOrderId 先作為 strategy 維度；後續 API 可獨立傳 strategyId / marketMakerId。
        TurnoverRecord record = new TurnoverRecord(
                null,
                trade.uid(),
                String.valueOf(trade.uid()),
                trade.symbol().code(),
                order == null ? null : order.getClientOrderId(),
                null,
                trade.orderId(),
                trade.matchId(),
                trade.seq(),
                trade.absQty(),
                trade.price(),
                trade.notional(),
                trade.ts(),
                null
        );
        turnoverStore.append(record);
    }

    public TurnoverSummary summarizeUser(long uid) {
        return summarize(uid, turnoverStore.findByUid(uid));
    }

    public TurnoverSummary summarizeMatch(long uid, String matchId) {
        return summarize(uid, turnoverStore.findByMatchId(matchId));
    }

    public TurnoverSummary summarize(
            long uid,
            String symbol,
            String strategyId,
            String marketMakerId,
            String matchId
    ) {
        List<TurnoverRecord> records = normalize(matchId) == null
                ? turnoverStore.findByUid(uid)
                : turnoverStore.findByMatchId(matchId);
        String normalizedSymbol = normalize(symbol);
        String normalizedStrategyId = normalize(strategyId);
        String normalizedMarketMakerId = normalize(marketMakerId);
        List<TurnoverRecord> filtered = records.stream()
                .filter(record -> record.uid() == uid)
                .filter(record -> normalizedSymbol == null || Objects.equals(normalizedSymbol, normalize(record.symbol())))
                .filter(record -> normalizedStrategyId == null || Objects.equals(normalizedStrategyId, normalize(record.strategyId())))
                .filter(record -> normalizedMarketMakerId == null || Objects.equals(normalizedMarketMakerId, normalize(record.marketMakerId())))
                .toList();
        TurnoverSummary summary = summarize(uid, filtered);
        return new TurnoverSummary(
                uid,
                normalizedSymbol,
                normalizedStrategyId,
                normalizedMarketMakerId,
                summary.tradeCount(),
                summary.quantity(),
                summary.notional()
        );
    }

    private static TurnoverSummary summarize(long uid, List<TurnoverRecord> records) {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal notional = BigDecimal.ZERO;
        long count = 0;
        for (TurnoverRecord record : records) {
            if (record.uid() != uid) continue;
            count++;
            quantity = quantity.add(record.quantity());
            notional = notional.add(record.notional());
        }
        return new TurnoverSummary(uid, null, null, null, count, quantity, notional);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
