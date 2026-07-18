package com.lumix.trading.core.futures.pnl;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.time.Instant;
import java.util.Objects;

/**
 * 指定 close price / quantity 下的 immutable realized PnL preview。
 *
 * preview 僅描述數學結果，不能被當成已成交、已平倉、已更新 position 或已結算的證據。
 */
public record FuturesSandboxRealizedPnlPreview(
        FuturesPositionId positionId,
        AssetSymbol settlementAsset,
        FuturesSandboxPnlPrice closePrice,
        FuturesPositionQuantity closeQuantity,
        MoneyAmount realizedPnl,
        Instant previewedAt
) {

    public FuturesSandboxRealizedPnlPreview {
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(settlementAsset, "settlementAsset must not be null");
        Objects.requireNonNull(closePrice, "closePrice must not be null");
        Objects.requireNonNull(closeQuantity, "closeQuantity must not be null");
        Objects.requireNonNull(realizedPnl, "realizedPnl must not be null");
        Objects.requireNonNull(previewedAt, "previewedAt must not be null");
    }
}
