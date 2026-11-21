package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 依 authoritative 商品快照驗證的限價單數值；通過不代表已接單、已 hold 或已成交。
 * 之後的接單服務必須保存 policyVersion，並另執行 owner／資金／商品狀態及合約風險檢查。
 */
public record TradingLimitOrderValues(BigDecimal price, BigDecimal quantity, BigDecimal notional, long policyVersion) {
    /** 先有界解析再檢查 tick、step 與上下限，金額不透過 binary floating point。 */
    public static TradingLimitOrderValues validate(TradingInstrumentRules rules, String priceInput, String quantityInput) {
        Objects.requireNonNull(rules, "authoritative instrument rules are required");
        BigDecimal price = TradingDecimal.positive(priceInput);
        BigDecimal quantity = TradingDecimal.positive(quantityInput);
        if (price.scale() > rules.priceScale() || quantity.scale() > rules.quantityScale()
                || price.remainder(rules.priceTick()).signum() != 0
                || quantity.remainder(rules.quantityStep()).signum() != 0
                || quantity.compareTo(rules.minimumQuantity()) < 0
                || quantity.compareTo(rules.maximumQuantity()) > 0) {
            throw new IllegalArgumentException("limit order violates instrument precision or quantity limits");
        }
        BigDecimal notional;
        try {
            notional = price.multiply(quantity).setScale(rules.quoteScale(), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("trade principal cannot be rounded implicitly", exception);
        }
        TradingDecimal.ledgerAmount(notional);
        if (notional.compareTo(rules.minimumNotional()) < 0 || notional.compareTo(rules.maximumNotional()) > 0) {
            throw new IllegalArgumentException("limit order violates notional limits");
        }
        return new TradingLimitOrderValues(price, quantity, notional, rules.policyVersion());
    }
}
