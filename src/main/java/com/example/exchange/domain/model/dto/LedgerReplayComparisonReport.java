/*
 * 檔案用途：ledger replay 與 account state 的結構化比對結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class LedgerReplayComparisonReport {

    private final long uid;

    private final String asset;

    private final boolean matched;

    private final WalletLedgerReplayResult replayResult;

    private final List<LedgerReplayComparisonIssue> issues;

    private final Instant comparedAt;
    public LedgerReplayComparisonReport(long uid, String asset, boolean matched, WalletLedgerReplayResult replayResult, List<LedgerReplayComparisonIssue> issues, Instant comparedAt) {
        this.uid = uid;
        this.asset = asset;
        this.matched = matched;
        this.replayResult = replayResult;
        this.issues = issues;
        this.comparedAt = comparedAt;
    }

    public long uid() {
        return uid;
    }

    public String asset() {
        return asset;
    }

    public boolean matched() {
        return matched;
    }

    public WalletLedgerReplayResult replayResult() {
        return replayResult;
    }

    public List<LedgerReplayComparisonIssue> issues() {
        return issues;
    }

    public Instant comparedAt() {
        return comparedAt;
    }
}