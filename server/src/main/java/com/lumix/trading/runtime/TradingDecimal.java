package com.lumix.trading.runtime;

import java.math.BigDecimal;

/** 交易 HTTP 輸入的有界十進位解析；先檢查文字長度與格式，再配置 BigDecimal。 */
public final class TradingDecimal {
    private TradingDecimal() { }

    /** 只接受非科學記號的正數，對齊 ledger NUMERIC(36,18)，不容忍 float/double 或隱含取整。 */
    public static BigDecimal positive(String text) {
        if (text == null || text.length() > 37
                || !text.matches("[0-9]{1,18}(?:\\.[0-9]{1,18})?")) {
            throw new IllegalArgumentException("trading decimal must be a bounded plain decimal");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.signum() <= 0) throw new IllegalArgumentException("trading decimal must be positive");
        value = value.stripTrailingZeros();
        return value.scale() < 0 ? value.setScale(0) : value;
    }

    /** 計算後再次確認可入帳範圍；禁止超出 ledger 的整數位或小數精度。 */
    public static BigDecimal ledgerAmount(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.precision() > 36 || value.scale() > 18
                || value.precision() - (long) value.scale() > 18) {
            throw new IllegalArgumentException("trading amount exceeds ledger precision");
        }
        return value;
    }
}
