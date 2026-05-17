/*
 * 檔案用途：應用層 Command，承載 UseCase 執行所需的輸入資料。
 */
package com.example.exchange.application.command;

import java.math.BigDecimal;

/**
 * 保證金劃轉指令
 * - Cross <-> Isolated 的資金移轉
 */
public record TransferMarginCommand(
        long uid,
        String symbol,
        boolean toIsolated,   // true: Cross -> Isolated；false: Isolated -> Cross
        BigDecimal amount
) {}
