package com.lumix.trading.core.futures.leverage;

/**
 * Futures 槓桿倍數。
 *
 * 使用正整數倍數表達 leverage，避免把 decimal、字串或浮點數帶進風險模型。
 */
public record FuturesLeverage(int multiplier) {

    /**
     * 建立 futures leverage value object。
     *
     * 這只是 convenience factory；真正的不變式仍由 canonical constructor 負責。
     */
    public static FuturesLeverage of(int multiplier) {
        return new FuturesLeverage(multiplier);
    }

    public FuturesLeverage {
        // Leverage 是風險模型的離散輸入，只允許正整數，不能接受 0、負數或小數語意。
        if (multiplier <= 0) {
            throw new IllegalArgumentException("multiplier must be greater than zero");
        }
    }
}
