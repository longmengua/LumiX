package com.lumix.trading.core.futures.pnl;

import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.time.Instant;
import java.util.Objects;

/**
 * Realized PnL 的純數學 close preview 輸入。
 *
 * 此 request 不代表 position close 已執行；它只讓 T04 鎖定開倉部位在指定價格與數量下的 PnL 算式。
 */
public record FuturesSandboxRealizedPnlPreviewRequest(
        FuturesAccount futuresAccount,
        FuturesPosition position,
        FuturesSandboxPnlPrice closePrice,
        FuturesPositionQuantity closeQuantity,
        Instant previewedAt
) {

    public FuturesSandboxRealizedPnlPreviewRequest {
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(closeQuantity, "closeQuantity must not be null");
        Objects.requireNonNull(previewedAt, "previewedAt must not be null");
        if (!futuresAccount.accountId().equals(position.futuresAccountId())) {
            throw new IllegalArgumentException("futuresAccount must own position");
        }
        if (closeQuantity.value().compareTo(position.quantity().value()) > 0) {
            throw new IllegalArgumentException("closeQuantity must not exceed position quantity");
        }
    }
}
