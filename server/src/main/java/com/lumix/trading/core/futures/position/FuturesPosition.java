package com.lumix.trading.core.futures.position;

import com.lumix.account.AccountId;

import java.time.Instant;
import java.util.Objects;

/**
 * Isolated margin futures position 的 immutable sandbox model。
 *
 * 這個 model 只描述單一 position 的 identity、account 歸屬、方向、數量、entry price 與生命週期。
 * 它不包含 cross margin pooling，也不包含 margin / PnL / liquidation / funding 計算。
 */
public record FuturesPosition(
        FuturesPositionId positionId,
        AccountId futuresAccountId,
        FuturesMarketSymbol marketSymbol,
        FuturesPositionSide side,
        FuturesPositionQuantity quantity,
        FuturesEntryPrice entryPrice,
        FuturesPositionStatus status,
        Instant openedAt,
        Instant updatedAt
) {

    /**
     * 建立新的開倉 position。
     *
     * 這只是 convenience factory，canonical constructor 仍然是 invariant boundary；
     * 直接建構與 rehydration 都會經過相同驗證。
     */
    public static FuturesPosition open(
            FuturesPositionId positionId,
            AccountId futuresAccountId,
            FuturesMarketSymbol marketSymbol,
            FuturesPositionSide side,
            FuturesPositionQuantity quantity,
            FuturesEntryPrice entryPrice,
            Instant openedAt
    ) {
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        return new FuturesPosition(
                positionId,
                futuresAccountId,
                marketSymbol,
                side,
                quantity,
                entryPrice,
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        );
    }

    public FuturesPosition {
        // Position 必須能被審計與重放，因此所有身份欄位、方向與生命週期欄位都要在建構時完整存在。
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(futuresAccountId, "futuresAccountId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(side, "side must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(entryPrice, "entryPrice must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (status != FuturesPositionStatus.OPEN) {
            throw new IllegalArgumentException("status must be OPEN");
        }
        if (updatedAt.isBefore(openedAt)) {
            throw new IllegalArgumentException("updatedAt must not be before openedAt");
        }
    }
}
