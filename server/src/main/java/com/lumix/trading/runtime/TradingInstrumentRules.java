package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 接單使用的具名、版本化商品規則。沒有預設費率、預設啟用市場或從 sandbox 推導的交易參數。
 *
 * <p>現貨與永續必須有不同 instrumentId；相同 underlying marketSymbol 不代表同一個可撮合商品。
 * 這個快照只描述數值規格，不取代行情 freshness、保證金／強平、owner 或可用額 gate。</p>
 */
public record TradingInstrumentRules(
        String instrumentId, Kind kind, String marketSymbol, String baseAsset, String quoteAsset,
        long policyVersion, int priceScale, int quantityScale, int quoteScale,
        BigDecimal priceTick, BigDecimal quantityStep,
        BigDecimal minimumQuantity, BigDecimal maximumQuantity,
        BigDecimal minimumNotional, BigDecimal maximumNotional,
        BigDecimal makerFeeRate, BigDecimal takerFeeRate, RoundingMode feeRounding
) {
    /** 商品類別不從 symbol 字尾猜測，避免現貨與 USDT 永續混簿。 */
    public enum Kind { SPOT, PERPETUAL }

    public TradingInstrumentRules {
        instrumentId = symbol(instrumentId, 64);
        marketSymbol = symbol(marketSymbol, 32);
        baseAsset = symbol(baseAsset, 32);
        quoteAsset = symbol(quoteAsset, 32);
        Objects.requireNonNull(kind, "instrument kind is required");
        Objects.requireNonNull(feeRounding, "fee rounding policy is required");
        if (baseAsset.equals(quoteAsset) || policyVersion <= 0) {
            throw new IllegalArgumentException("instrument identity or policy version is invalid");
        }
        if (priceScale < 0 || priceScale > 18 || quantityScale < 0 || quantityScale > 18
                || quoteScale < 0 || quoteScale > 18) {
            throw new IllegalArgumentException("instrument scales must be within ledger precision");
        }
        priceTick = positive(priceTick);
        quantityStep = positive(quantityStep);
        minimumQuantity = positive(minimumQuantity);
        maximumQuantity = positive(maximumQuantity);
        minimumNotional = positive(minimumNotional);
        maximumNotional = positive(maximumNotional);
        makerFeeRate = rate(makerFeeRate);
        takerFeeRate = rate(takerFeeRate);
        if (priceTick.scale() > priceScale || quantityStep.scale() > quantityScale
                || minimumQuantity.compareTo(maximumQuantity) > 0
                || minimumNotional.compareTo(maximumNotional) > 0
                || minimumQuantity.remainder(quantityStep).signum() != 0
                || maximumQuantity.remainder(quantityStep).signum() != 0) {
            throw new IllegalArgumentException("instrument steps and limits are inconsistent");
        }
        // 每個合法 tick × step 都必須能以 quote 最小單位精確結算，不在撮合時偷偷截掉交易本金。
        try {
            priceTick.multiply(quantityStep).setScale(quoteScale, RoundingMode.UNNECESSARY);
            minimumNotional.setScale(quoteScale, RoundingMode.UNNECESSARY);
            maximumNotional.setScale(quoteScale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("instrument amounts cannot settle exactly in quote asset", exception);
        }
        // 費用可為零，但必須由呼叫端明確指定；UNNECESSARY 無法保證任意成交費用可落在 quote 精度。
        if (feeRounding == RoundingMode.UNNECESSARY) {
            throw new IllegalArgumentException("explicit fee rounding is required");
        }
    }

    private static String symbol(String value, int maximum) {
        if (value == null || value.length() > maximum || !value.matches("[A-Z0-9][A-Z0-9:_-]*")) {
            throw new IllegalArgumentException("instrument identifier is invalid");
        }
        return value;
    }

    private static BigDecimal positive(BigDecimal value) {
        Objects.requireNonNull(value, "instrument amount is required");
        if (value.signum() <= 0) throw new IllegalArgumentException("instrument amount must be positive");
        TradingDecimal.ledgerAmount(value);
        return value.stripTrailingZeros();
    }

    private static BigDecimal rate(BigDecimal value) {
        Objects.requireNonNull(value, "fee rate is required");
        TradingDecimal.ledgerAmount(value);
        if (value.compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("fee rate exceeds one");
        return value.stripTrailingZeros();
    }
}
