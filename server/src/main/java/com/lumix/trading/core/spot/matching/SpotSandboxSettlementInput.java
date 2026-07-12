package com.lumix.trading.core.spot.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Spot sandbox settlement input。
 *
 * 這份 record 只表示後續 settlement 的輸入資料，不代表 settlement runtime、ledger posting 或 balance 更新已完成。
 */
public record SpotSandboxSettlementInput(
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
     * 建立不可變的 settlement input。
     *
     * 這裡只保留 sandbox trade 轉給後續流程所需的最小資訊，不代表真正結算已執行。
     */
    public SpotSandboxSettlementInput {
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
