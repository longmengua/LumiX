package com.lumix.trading.core.futures.sandbox.contract;

import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import java.util.Objects;

/**
 * 單一 market 的受限 futures sandbox contract 範圍。
 *
 * T06 以型別只保留一個 market，避免呼叫端把多個合約混入同一次 sandbox inspection。
 * 此範圍不包含 public access、真實資金、撮合或任何可執行的交易設定。
 */
public record RestrictedFuturesSandboxContract(FuturesMarketSymbol marketSymbol) {

    public RestrictedFuturesSandboxContract {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
    }
}
