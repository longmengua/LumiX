package com.lumix.trading.runtime;

import com.lumix.marketfeed.binance.BinanceFuturesReferencePriceFeed;
import com.lumix.trading.core.futures.margin.FuturesCollateralValuation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** 逐倉新增風險的確定性入場規則；所有行情、抵押品與兌付池餘額由後端提供。 */
public final class FuturesOrderRiskGate {
    private FuturesOrderRiskGate() { }

    /** 可回放的接單決策；實際預留與帳本提交仍須由同一資料庫交易完成。 */
    public record Decision(BigDecimal orderNotionalUsdt, BigDecimal projectedNotionalUsdt,
                           BigDecimal initialMarginUsdt, BigDecimal maintenanceMarginUsdt,
                           BigDecimal maximumFeeUsdt, BigDecimal collateralValueUsdt,
                           String markSource, Instant markSourceAt, long collateralRatePolicyVersion) { }

    /**
     * 價格採委託價與標記價的較高者估計新增風險；不得以低價委託繞過名義額上限。
     * 非 USDT 抵押的 USDT 兌付能力必須同時受政策額度與實際可用池餘額限制。
     */
    public static Decision admit(FuturesRiskPolicy policy,
                                 BinanceFuturesReferencePriceFeed.Snapshot reference,
                                 FuturesCollateralValuation.Rate collateralRate,
                                 BigDecimal orderQuantity, BigDecimal orderPriceUsdt,
                                 BigDecimal requestedLeverage,
                                 BigDecimal existingPositionNotionalUsdt,
                                 BigDecimal collateralAmount, BigDecimal outstandingConversionUsdt,
                                 BigDecimal availableConversionPoolUsdt, Instant now) {
        Objects.requireNonNull(policy, "futures risk policy is required");
        Objects.requireNonNull(reference, "fresh reference price is required");
        Objects.requireNonNull(now, "risk decision time is required");
        var rules = policy.execution().rules();
        if (!reference.source().equals(policy.markPriceSource())
                || !reference.source().equals(policy.indexPriceSource())
                || !reference.symbol().equals(rules.baseAsset() + rules.quoteAsset())
                || !fresh(reference.sourceAt(), now, policy.maximumPriceAge())
                || !fresh(reference.receivedAt(), now, policy.maximumPriceAge()))
            throw new TradingOrderRejectedException("futures reference price is unavailable or stale");
        positive(reference.markPrice(), "mark price");
        positive(reference.indexPrice(), "index price");
        positive(orderQuantity, "order quantity");
        positive(orderPriceUsdt, "order price");
        positive(requestedLeverage, "requested leverage");
        TradingLimitOrderValues.validate(rules, orderPriceUsdt.toPlainString(), orderQuantity.toPlainString());
        if (requestedLeverage.scale() < 0 || requestedLeverage.scale() > 8
                || requestedLeverage.precision() > 18)
            throw new IllegalArgumentException("leverage exceeds persistent precision");
        if (requestedLeverage.compareTo(BigDecimal.ONE) < 0
                || requestedLeverage.compareTo(policy.maximumLeverage()) > 0)
            throw new TradingOrderRejectedException("requested leverage exceeds policy");
        nonNegative(existingPositionNotionalUsdt, "existing position notional");
        nonNegative(collateralAmount, "available collateral");
        nonNegative(outstandingConversionUsdt, "outstanding conversion exposure");
        nonNegative(availableConversionPoolUsdt, "available conversion pool");

        BigDecimal worstPrice = reference.markPrice().max(orderPriceUsdt);
        BigDecimal orderNotional = worstPrice.multiply(orderQuantity).setScale(rules.quoteScale(), RoundingMode.CEILING);
        BigDecimal projected = existingPositionNotionalUsdt.add(orderNotional);
        if (projected.compareTo(policy.maximumPositionNotionalUsdt()) > 0)
            throw new TradingOrderRejectedException("futures position notional limit exceeded");
        BigDecimal initial = orderNotional.divide(requestedLeverage, rules.quoteScale(), RoundingMode.CEILING);
        BigDecimal maintenance = orderNotional.multiply(policy.maintenanceMarginRate())
                .setScale(rules.quoteScale(), RoundingMode.CEILING);
        BigDecimal maximumFee = orderNotional.multiply(rules.makerFeeRate().max(rules.takerFeeRate()))
                .setScale(rules.quoteScale(), RoundingMode.CEILING);
        BigDecimal collateralValue;
        long rateVersion = 0;
        if ("USDT".equals(policy.collateralAsset())) {
            if (collateralRate != null) throw new IllegalArgumentException("USDT collateral needs no external rate");
            collateralValue = collateralAmount.multiply(BigDecimal.ONE.subtract(policy.collateralHaircut()))
                    .setScale(rules.quoteScale(), RoundingMode.DOWN);
        } else {
            if (collateralRate == null || !policy.collateralAsset().equals(collateralRate.collateralAsset())
                    || policy.collateralHaircut().compareTo(collateralRate.haircut()) != 0)
                throw new TradingOrderRejectedException("collateral conversion policy is unavailable");
            collateralValue = FuturesCollateralValuation.valueInUsdt(collateralAmount, collateralRate,
                    now, rules.quoteScale());
            rateVersion = collateralRate.policyVersion();
            BigDecimal projectedConversion = outstandingConversionUsdt.add(initial).add(maximumFee);
            if (projectedConversion.compareTo(policy.maximumConversionExposureUsdt()) > 0
                    || initial.add(maximumFee).compareTo(availableConversionPoolUsdt) > 0)
                throw new TradingOrderRejectedException("USDT conversion pool capacity is insufficient");
        }
        if (collateralValue.compareTo(initial.add(maximumFee)) < 0)
            throw new TradingOrderRejectedException("isolated collateral is insufficient");
        return new Decision(orderNotional, projected, initial, maintenance, maximumFee, collateralValue,
                reference.source(), reference.sourceAt(), rateVersion);
    }

    private static boolean fresh(Instant observed, Instant now, Duration maximumAge) {
        return observed != null && !observed.isAfter(now)
                && Duration.between(observed, now).compareTo(maximumAge) <= 0;
    }

    private static void positive(BigDecimal value, String label) {
        nonNegative(value, label);
        if (value.signum() == 0) throw new IllegalArgumentException(label + " must be positive");
    }

    private static void nonNegative(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " is required");
        TradingDecimal.ledgerAmount(value);
        if (value.signum() < 0) throw new IllegalArgumentException(label + " cannot be negative");
    }
}
