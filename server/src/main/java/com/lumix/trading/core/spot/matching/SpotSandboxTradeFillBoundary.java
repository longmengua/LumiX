package com.lumix.trading.core.spot.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Spot sandbox trade / fill result 的設計邊界。
 *
 * 這個 boundary 只把 sandbox trade 轉成後續 settlement input，不代表真正 settlement runtime 已完成。
 */
public final class SpotSandboxTradeFillBoundary {

    /**
     * 建立 sandbox trade / fill result。
     *
     * 這裡只產出記憶體內的 sandbox result，不會寫任何 DB 或嘗試做 settlement。
     */
    public SpotSandboxTradeFill createTradeFill(
            String sandboxTradeId,
            String marketSymbol,
            String buySandboxOrderId,
            String sellSandboxOrderId,
            String buyAccountId,
            String sellAccountId,
            BigDecimal price,
            BigDecimal quantity,
            Instant tradeTime,
            SpotSandboxTradePriceRule tradePriceRule
    ) {
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        BigDecimal quoteAmount = price.multiply(quantity);

        return new SpotSandboxTradeFill(
                sandboxTradeId,
                marketSymbol,
                buySandboxOrderId,
                sellSandboxOrderId,
                buyAccountId,
                sellAccountId,
                price,
                quantity,
                quoteAmount,
                tradeTime,
                tradePriceRule,
                SpotSandboxTradeFillStatus.CREATED_FOR_SANDBOX
        );
    }

    /**
     * 把 sandbox trade / fill 轉成 settlement input。
     *
     * 這裡只保留後續 settlement 所需的輸入資料，狀態仍明確標示為尚未開始結算。
     */
    public SpotSandboxSettlementInput toSettlementInput(SpotSandboxTradeFill tradeFill) {
        Objects.requireNonNull(tradeFill, "tradeFill must not be null");
        return new SpotSandboxSettlementInput(
                tradeFill.sandboxTradeId(),
                tradeFill.marketSymbol(),
                tradeFill.buySandboxOrderId(),
                tradeFill.sellSandboxOrderId(),
                tradeFill.buyAccountId(),
                tradeFill.sellAccountId(),
                tradeFill.price(),
                tradeFill.quantity(),
                tradeFill.quoteAmount(),
                tradeFill.tradeTime(),
                tradeFill.tradePriceRule(),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        );
    }

    /**
     * 把一組 sandbox trade / fill 轉成 settlement input。
     *
     * 這裡只做清單映射，不代表真正 settlement runtime 已完成。
     */
    public List<SpotSandboxSettlementInput> toSettlementInputs(List<SpotSandboxTradeFill> tradeFills) {
        Objects.requireNonNull(tradeFills, "tradeFills must not be null");
        return tradeFills.stream().map(this::toSettlementInput).toList();
    }
}
