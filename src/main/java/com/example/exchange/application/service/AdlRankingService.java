/*
 * 檔案用途：應用服務，計算 production ADL queue 的 deterministic ranking。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AdlRankedPosition;
import com.example.exchange.domain.model.dto.AdlRankingCandidate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ADL ranking service。
 *
 * <p>排序規則固定為：獲利率高者優先、有效槓桿高者優先、名義價值大者優先、uid 小者優先。
 * 最後的 uid tie-breaker 讓同分候選人在 replay 或多節點測試中仍保持 deterministic order。</p>
 */
@Service
public class AdlRankingService {

    private static final int SCALE = 18;

    /**
     * 依 ADL 優先順序排序候選倉位。
     *
     * <p>虧損或零倉位不會進入 ADL queue，因為 ADL 只應強制減掉高獲利、高槓桿對手方。</p>
     */
    public List<AdlRankedPosition> rank(List<AdlRankingCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        AtomicInteger rank = new AtomicInteger(1);
        return candidates.stream()
                .filter(AdlRankingService::isRankable)
                .map(this::toRankedWithoutRank)
                .filter(position -> position.profitRate().signum() > 0)
                .sorted(Comparator
                        .comparing(AdlRankedPosition::profitRate, Comparator.reverseOrder())
                        .thenComparing(AdlRankedPosition::effectiveLeverage, Comparator.reverseOrder())
                        .thenComparing(AdlRankedPosition::notional, Comparator.reverseOrder())
                        .thenComparingLong(AdlRankedPosition::uid))
                .map(position -> new AdlRankedPosition(
                        rank.getAndIncrement(),
                        position.uid(),
                        position.symbol(),
                        position.profitRate(),
                        position.effectiveLeverage(),
                        position.notional()
                ))
                .toList();
    }

    private AdlRankedPosition toRankedWithoutRank(AdlRankingCandidate candidate) {
        BigDecimal qtyAbs = candidate.qty().abs();
        BigDecimal notional = qtyAbs.multiply(candidate.markPrice()).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal pnl = unrealizedPnl(candidate);
        BigDecimal entryNotional = qtyAbs.multiply(candidate.entryPrice());
        BigDecimal profitRate = entryNotional.signum() == 0
                ? BigDecimal.ZERO
                : pnl.divide(entryNotional, SCALE, RoundingMode.HALF_UP);
        BigDecimal margin = safe(candidate.margin());
        BigDecimal effectiveLeverage = margin.signum() <= 0
                ? safe(candidate.leverage())
                : notional.divide(margin, SCALE, RoundingMode.HALF_UP);
        return new AdlRankedPosition(
                0,
                candidate.uid(),
                normalize(candidate.symbol()),
                profitRate,
                effectiveLeverage,
                notional
        );
    }

    private static BigDecimal unrealizedPnl(AdlRankingCandidate candidate) {
        BigDecimal qtyAbs = candidate.qty().abs();
        if (candidate.qty().signum() > 0) {
            return candidate.markPrice().subtract(candidate.entryPrice()).multiply(qtyAbs);
        }
        return candidate.entryPrice().subtract(candidate.markPrice()).multiply(qtyAbs);
    }

    private static boolean isRankable(AdlRankingCandidate candidate) {
        return candidate != null
                && candidate.qty() != null
                && candidate.qty().signum() != 0
                && candidate.entryPrice() != null
                && candidate.entryPrice().signum() > 0
                && candidate.markPrice() != null
                && candidate.markPrice().signum() > 0;
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
