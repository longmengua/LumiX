package com.lumix.trading.core.futures.pnl;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import java.time.Instant;
import java.util.Objects;

/**
 * 以單次外部 mark price 計算出的 immutable unrealized PnL snapshot。
 *
 * 此 snapshot 不會寫回 position、balance 或 ledger，也不宣告 mark price 來自任何正式行情來源。
 */
public record FuturesSandboxUnrealizedPnlSnapshot(
        FuturesPositionId positionId,
        AssetSymbol settlementAsset,
        FuturesSandboxPnlPrice markPrice,
        MoneyAmount unrealizedPnl,
        Instant valuedAt
) {

    public FuturesSandboxUnrealizedPnlSnapshot {
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(settlementAsset, "settlementAsset must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(unrealizedPnl, "unrealizedPnl must not be null");
        Objects.requireNonNull(valuedAt, "valuedAt must not be null");
    }
}
