package com.lumix.trading.core.futures.margin;

import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;

import java.util.Objects;

/**
 * Isolated initial-margin sufficiency gate 的 immutable 輸入快照。
 *
 * 這個 request 只保存 account、leverage、market、quantity、entry price 與可用 settlement margin，
 * 不讀取外部狀態，也不接 order、position、wallet、ledger 或 repository 參考。
 */
public record IsolatedMarginCheckRequest(
        FuturesAccount futuresAccount,
        IsolatedLeverageConfig leverageConfig,
        FuturesMarketSymbol marketSymbol,
        FuturesPositionQuantity quantity,
        FuturesEntryPrice entryPrice,
        AssetSymbol availableMarginAsset,
        MoneyAmount availableMargin
) {

    public IsolatedMarginCheckRequest {
        // T04 只接受已正規化完成的 immutable domain model，避免呼叫端用裸 BigDecimal 或外部查詢物件繞過既有不變式。
        Objects.requireNonNull(futuresAccount, "futuresAccount must not be null");
        Objects.requireNonNull(leverageConfig, "leverageConfig must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(entryPrice, "entryPrice must not be null");
        Objects.requireNonNull(availableMarginAsset, "availableMarginAsset must not be null");
        Objects.requireNonNull(availableMargin, "availableMargin must not be null");
        if (availableMargin.isNegative()) {
            throw new IllegalArgumentException("availableMargin must not be negative");
        }
    }
}
