/*
 * 檔案用途：領域 DTO，回傳 wallet ledger replay 後的帳戶資產狀態與驗證結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WalletLedgerReplayResult(
        long uid,
        String asset,
        int entryCount,
        int postingCount,
        BigDecimal balance,
        BigDecimal available,
        BigDecimal orderHold,
        BigDecimal positionMargin,
        boolean balanced,
        List<String> issues,
        Instant replayedAt
) {
}
