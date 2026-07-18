package com.lumix.trading.core.futures.pnl;

import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import java.time.Instant;
import java.util.Objects;

/**
 * Unrealized PnL 的 immutable sandbox valuation 輸入。
 *
 * 使用 futures account 提供 settlement asset 與 account ownership 驗證，但不讀取真實餘額或任何外部價格服務。
 */
public record FuturesSandboxUnrealizedPnlRequest(
        FuturesAccount futuresAccount,
        FuturesPosition position,
        FuturesSandboxPnlPrice markPrice,
        Instant valuedAt
) {

    public FuturesSandboxUnrealizedPnlRequest {
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(valuedAt, "valuedAt must not be null");
        if (!futuresAccount.accountId().equals(position.futuresAccountId())) {
            throw new IllegalArgumentException("futuresAccount must own position");
        }
    }
}
