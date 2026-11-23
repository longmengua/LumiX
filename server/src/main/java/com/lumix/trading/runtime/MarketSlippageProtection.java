package com.lumix.trading.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 市價 IOC 只以真實站內對手價計算客戶最差成交價；外部參考深度不可充當可成交價。 */
final class MarketSlippageProtection {
    private MarketSlippageProtection() { }

    static BigDecimal bound(BigDecimal best, String side, BigDecimal tick, BigDecimal policyMaximum,
                            String mode, String rawValue) {
        boolean buy = "BUY".equals(side);
        if ((mode == null) != (rawValue == null))
            throw new IllegalArgumentException("slippage mode and value must be supplied together");
        BigDecimal distance;
        if (mode == null) {
            distance = best.multiply(policyMaximum);
        } else {
            if (!"PERCENT".equals(mode) && !"FIXED_USDT".equals(mode))
                throw new IllegalArgumentException("unsupported slippage mode");
            BigDecimal value = TradingDecimal.positive(rawValue);
            distance = "PERCENT".equals(mode) ? best.multiply(value.movePointLeft(2)) : value;
            if (distance.compareTo(best.multiply(policyMaximum)) > 0)
                throw new TradingOrderRejectedException("slippage tolerance exceeds market policy");
        }
        BigDecimal rawBound = buy ? best.add(distance) : best.subtract(distance);
        if (rawBound.signum() <= 0)
            throw new TradingOrderRejectedException("slippage bound is invalid");
        // 買入向下、賣出向上對齊 tick，不能因取整而突破客戶批准的最差價格。
        return rawBound.divide(tick, 0, buy ? RoundingMode.FLOOR : RoundingMode.CEILING).multiply(tick);
    }
}
