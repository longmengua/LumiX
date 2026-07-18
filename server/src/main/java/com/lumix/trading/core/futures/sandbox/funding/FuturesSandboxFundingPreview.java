package com.lumix.trading.core.futures.sandbox.funding;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import java.time.Instant;
import java.util.Objects;

/**
 * Funding 的 immutable 純試算結果。
 *
 * signedFundingAmount 為 position 視角：正值代表應收，負值代表應付；結果僅供展示或後續審核，
 * 不可直接視為 account balance、ledger entry 或 settlement instruction。
 */
public record FuturesSandboxFundingPreview(
        FuturesPositionId positionId,
        AssetSymbol settlementAsset,
        FuturesSandboxMockMarkPrice markPrice,
        FuturesSandboxFundingRate fundingRate,
        MoneyAmount signedFundingAmount,
        FuturesSandboxFundingDirection direction,
        Instant fundingAt
) {

    public FuturesSandboxFundingPreview {
        Objects.requireNonNull(positionId, "positionId must not be null");
        Objects.requireNonNull(settlementAsset, "settlementAsset must not be null");
        Objects.requireNonNull(markPrice, "markPrice must not be null");
        Objects.requireNonNull(fundingRate, "fundingRate must not be null");
        Objects.requireNonNull(signedFundingAmount, "signedFundingAmount must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(fundingAt, "fundingAt must not be null");
    }
}
