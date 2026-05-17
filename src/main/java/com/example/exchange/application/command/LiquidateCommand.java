package com.example.exchange.application.command;

import java.math.BigDecimal;

/**
 * 強平觸發指令
 * - 真實情境會有更多參數（標記價、保險基金、保證金率等）
 */
public record LiquidateCommand(long uid, String symbol, BigDecimal markPrice) {
    public LiquidateCommand(long uid, String symbol) {
        this(uid, symbol, null);
    }
}
