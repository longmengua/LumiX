/*
 * 檔案用途：領域 DTO，回傳 wallet ledger replay 後的帳戶資產狀態與驗證結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class WalletLedgerReplayResult {

    private final long uid;

    private final String asset;

    private final int entryCount;

    private final int postingCount;

    private final BigDecimal balance;

    private final BigDecimal available;

    private final BigDecimal orderHold;

    private final BigDecimal positionMargin;

    private final boolean balanced;

    private final List<String> issues;

    private final Instant replayedAt;
    public WalletLedgerReplayResult(long uid, String asset, int entryCount, int postingCount, BigDecimal balance, BigDecimal available, BigDecimal orderHold, BigDecimal positionMargin, boolean balanced, List<String> issues, Instant replayedAt) {
        this.uid = uid;
        this.asset = asset;
        this.entryCount = entryCount;
        this.postingCount = postingCount;
        this.balance = balance;
        this.available = available;
        this.orderHold = orderHold;
        this.positionMargin = positionMargin;
        this.balanced = balanced;
        this.issues = issues;
        this.replayedAt = replayedAt;
    }

    public long uid() {
        return uid;
    }

    public String asset() {
        return asset;
    }

    public int entryCount() {
        return entryCount;
    }

    public int postingCount() {
        return postingCount;
    }

    public BigDecimal balance() {
        return balance;
    }

    public BigDecimal available() {
        return available;
    }

    public BigDecimal orderHold() {
        return orderHold;
    }

    public BigDecimal positionMargin() {
        return positionMargin;
    }

    public boolean balanced() {
        return balanced;
    }

    public List<String> issues() {
        return issues;
    }

    public Instant replayedAt() {
        return replayedAt;
    }
}