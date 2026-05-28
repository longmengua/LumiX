/*
 * 檔案用途：ledger replay 與 account state 的結構化比對結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record LedgerReplayComparisonReport(
        long uid,
        String asset,
        boolean matched,
        WalletLedgerReplayResult replayResult,
        List<LedgerReplayComparisonIssue> issues,
        Instant comparedAt
) {
}
