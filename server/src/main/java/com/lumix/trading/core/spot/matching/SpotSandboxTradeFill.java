package com.lumix.trading.core.spot.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Spot sandbox trade / fill result。
 *
 * 這份 record 只存在於 sandbox matching runtime，不能被誤解為已持久化、已 settlement 或已 ledger posted。
 */
public record SpotSandboxTradeFill(
        String sandboxTradeId,
        String marketSymbol,
        String buySandboxOrderId,
        String sellSandboxOrderId,
        String buyAccountId,
        String sellAccountId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal quoteAmount,
        Instant tradeTime,
        SpotSandboxTradePriceRule tradePriceRule,
        SpotSandboxTradeFillStatus status
) {

    /**
     * 建立不可變的 trade / fill 結果。
     *
     * 這裡只做必要 null 檢查，避免 sandbox result 被意外建立成半成品。
     */
    public SpotSandboxTradeFill {
        Objects.requireNonNull(sandboxTradeId, "sandboxTradeId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(buySandboxOrderId, "buySandboxOrderId must not be null");
        Objects.requireNonNull(sellSandboxOrderId, "sellSandboxOrderId must not be null");
        Objects.requireNonNull(buyAccountId, "buyAccountId must not be null");
        Objects.requireNonNull(sellAccountId, "sellAccountId must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(quoteAmount, "quoteAmount must not be null");
        Objects.requireNonNull(tradeTime, "tradeTime must not be null");
        Objects.requireNonNull(tradePriceRule, "tradePriceRule must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
