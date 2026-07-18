package com.lumix.trading.core.futures.sandbox.funding;

import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.time.Instant;
import java.util.Objects;

/**
 * 一次性的 Futures sandbox funding preview 輸入。
 *
 * 呼叫端必須明確帶入 fundingAt，避免本 task 暗中實作 funding schedule 或使用系統時鐘。
 */
public record FuturesSandboxFundingPreviewRequest(
        FuturesAccount futuresAccount,
        FuturesPosition position,
        FuturesSandboxMockMarkPrice markPrice,
        FuturesSandboxFundingRate fundingRate,
        Instant fundingAt
) {

    public FuturesSandboxFundingPreviewRequest {
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(fundingRate, "fundingRate must not be null");
        Objects.requireNonNull(fundingAt, "fundingAt must not be null");
        if (!futuresAccount.accountId().equals(position.futuresAccountId())) {
            throw new IllegalArgumentException("futuresAccount must own position");
        }
        if (!position.marketSymbol().equals(markPrice.marketSymbol())) {
            throw new IllegalArgumentException("markPrice market must match position market");
        }
        if (fundingAt.isBefore(markPrice.publishedAt())) {
            throw new IllegalArgumentException("fundingAt must not be before markPrice publishedAt");
        }
    }
}
