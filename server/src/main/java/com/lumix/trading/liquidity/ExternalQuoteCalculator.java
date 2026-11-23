package com.lumix.trading.liquidity;

import com.lumix.trading.runtime.TradingDecimal;
import com.lumix.trading.runtime.TradingInstrumentRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 依來源深度計算客戶報價，價格向外取整，避免 tick 取整吃掉預定價差。
 * 此純函式不接單、不保留外部流動性、不發布可成交簿；外部成交與帳本須另有證據鏈。
 */
public final class ExternalQuoteCalculator {
    private ExternalQuoteCalculator() { }

    /**
     * 只對新鮮且商品一致的快照產生預覽。maxRoutableQuantity 須由上層依外部帳戶可用量與風控額度計算；
     * 此處對買賣兩側各自設總量上限，不把來源全部掛單直接宣稱為可成交流動性。
     */
    public static Optional<QuoteBook> quote(ExternalLiquiditySource.Snapshot snapshot,
                                            TradingInstrumentRules instrument, ExternalQuotePolicy policy,
                                            BigDecimal maxRoutableQuantity, Instant now, Duration maxAge) {
        Objects.requireNonNull(snapshot, "source snapshot is required");
        Objects.requireNonNull(instrument, "instrument is required");
        Objects.requireNonNull(policy, "quote policy is required");
        Objects.requireNonNull(now, "current time is required");
        Objects.requireNonNull(maxAge, "maximum age is required");
        if (maxAge.isZero() || maxAge.isNegative()) throw new IllegalArgumentException("maximum age must be positive");
        if (maxRoutableQuantity == null || maxRoutableQuantity.signum() <= 0)
            throw new IllegalArgumentException("routable quantity cap must be positive");
        TradingDecimal.ledgerAmount(maxRoutableQuantity);
        if (snapshot.kind() != instrument.kind() || !instrument.marketSymbol().equals(snapshot.marketSymbol())
                || !instrument.baseAsset().equals(policy.asset()) || snapshot.source() == null
                || !snapshot.source().matches("[A-Z0-9][A-Z0-9_-]{0,63}"))
            throw new IllegalArgumentException("source, instrument or quote policy mismatch");
        if (snapshot.sourceAt() == null || snapshot.receivedAt() == null || snapshot.sourceAt().isAfter(snapshot.receivedAt())
                || snapshot.receivedAt().isAfter(now) || snapshot.sourceAt().isAfter(now)
                || Duration.between(snapshot.sourceAt(), now).compareTo(maxAge) > 0
                || Duration.between(snapshot.receivedAt(), now).compareTo(maxAge) > 0)
            return Optional.empty();
        validateLevels(snapshot.bids(), false);
        validateLevels(snapshot.asks(), true);
        if (!snapshot.bids().isEmpty() && !snapshot.asks().isEmpty()
                && snapshot.bids().getFirst().price().compareTo(snapshot.asks().getFirst().price()) >= 0)
            throw new IllegalArgumentException("source book is crossed");
        List<QuoteLevel> bids = project(snapshot.bids(), false, instrument.priceTick(),
                instrument.quantityStep(), policy.effectiveRate(), policy.effectiveFixedUsdt(), maxRoutableQuantity);
        List<QuoteLevel> asks = project(snapshot.asks(), true, instrument.priceTick(),
                instrument.quantityStep(), policy.effectiveRate(), policy.effectiveFixedUsdt(), maxRoutableQuantity);
        return Optional.of(new QuoteBook(snapshot.source(), snapshot.kind(), snapshot.marketSymbol(),
                snapshot.sourceAt(), snapshot.receivedAt(), policy.version(), bids, asks));
    }

    private static void validateLevels(List<ExternalLiquiditySource.Level> levels, boolean ask) {
        if (levels == null || levels.size() > 1000) throw new IllegalArgumentException("source depth is invalid");
        BigDecimal previous = null;
        for (var level : levels) {
            if (level == null || level.price() == null || level.quantity() == null
                    || level.price().signum() <= 0 || level.quantity().signum() <= 0)
                throw new IllegalArgumentException("source level is invalid");
            TradingDecimal.ledgerAmount(level.price());
            TradingDecimal.ledgerAmount(level.quantity());
            if (previous != null && (ask ? level.price().compareTo(previous) <= 0
                    : level.price().compareTo(previous) >= 0))
                throw new IllegalArgumentException("source depth is unordered");
            previous = level.price();
        }
    }

    private static List<QuoteLevel> project(List<ExternalLiquiditySource.Level> levels, boolean ask,
                                            BigDecimal priceTick, BigDecimal quantityStep,
                                            BigDecimal rate, BigDecimal fixedUsdt, BigDecimal cap) {
        Map<BigDecimal, BigDecimal> grouped = new LinkedHashMap<>();
        BigDecimal remaining = cap;
        for (var level : levels) {
            if (grouped.size() >= 20 || remaining.signum() <= 0) break;
            BigDecimal quantity = level.quantity().min(remaining).divide(quantityStep, 0, RoundingMode.DOWN)
                    .multiply(quantityStep);
            if (quantity.signum() == 0) continue;
            BigDecimal adjusted = level.price().multiply(ask ? BigDecimal.ONE.add(rate) : BigDecimal.ONE.subtract(rate))
                    .add(ask ? fixedUsdt : fixedUsdt.negate());
            BigDecimal price = adjusted.divide(priceTick, 0, ask ? RoundingMode.CEILING : RoundingMode.FLOOR)
                    .multiply(priceTick);
            if (price.signum() <= 0) throw new IllegalArgumentException("quoted price is not positive");
            TradingDecimal.ledgerAmount(price);
            grouped.merge(price, quantity, BigDecimal::add);
            remaining = remaining.subtract(quantity);
        }
        List<QuoteLevel> result = new ArrayList<>(grouped.size());
        grouped.forEach((price, quantity) -> result.add(new QuoteLevel(price, quantity)));
        return List.copyOf(result);
    }

    public record QuoteLevel(BigDecimal price, BigDecimal quantity) { }

    public record QuoteBook(String source, TradingInstrumentRules.Kind kind, String marketSymbol,
                            Instant sourceAt, Instant receivedAt, long policyVersion,
                            List<QuoteLevel> bids, List<QuoteLevel> asks) { }
}
