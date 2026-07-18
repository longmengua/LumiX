package com.lumix.trading.core.futures.sandbox.market;

import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 僅供 Futures sandbox 使用的 immutable mock mark-price snapshot。
 *
 * 這不是行情服務，也不保存最新價格；呼叫端必須為每次 valuation 明確帶入價格與發布時間，
 * 使試算可以被重放與審計，而不依賴 clock、cache 或 production market runtime。
 */
public record FuturesSandboxMockMarkPrice(
        FuturesMarketSymbol marketSymbol,
        BigDecimal price,
        FuturesSandboxMockMarkPriceSource source,
        Instant publishedAt
) {

    public FuturesSandboxMockMarkPrice {
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        price = price.stripTrailingZeros();
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }
}
