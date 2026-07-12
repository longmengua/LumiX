package com.lumix.trading.core.spot.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox trade/fill result boundary 只停在 sandbox input，不會偷接成正式 settlement runtime。
 */
class SpotSandboxTradeFillBoundaryTest {

    private final SpotSandboxTradeFillBoundary boundary = new SpotSandboxTradeFillBoundary();

    /**
     * 確認 trade/fill result 可以轉成 settlement input，且狀態清楚標示尚未開始結算。
     *
     * 這個 case 必須存在，因為後續 settlement sandbox 只能接收明確的 trade input。
     */
    @Test
    void createsSettlementInputFromSandboxTradeFill() {
        SpotSandboxTradeFill tradeFill = boundary.createTradeFill(
                "sandbox-trade-1",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct-1",
                "sell-acct-1",
                new BigDecimal("50020.00"),
                new BigDecimal("0.10"),
                Instant.parse("2026-07-12T03:20:00Z"),
                SpotSandboxTradePriceRule.MAKER_PRICE
        );

        SpotSandboxSettlementInput settlementInput = boundary.toSettlementInput(tradeFill);
        List<SpotSandboxSettlementInput> settlementInputs = boundary.toSettlementInputs(List.of(tradeFill));

        assertEquals(new BigDecimal("5002.0000"), tradeFill.quoteAmount());
        assertEquals(SpotSandboxTradeFillStatus.CREATED_FOR_SANDBOX, tradeFill.status());
        assertEquals("sandbox-trade-1", settlementInput.sandboxTradeId());
        assertEquals("BTC-USDT", settlementInput.marketSymbol());
        assertEquals(new BigDecimal("50020.00"), settlementInput.price());
        assertEquals(new BigDecimal("0.10"), settlementInput.quantity());
        assertEquals(new BigDecimal("5002.0000"), settlementInput.quoteAmount());
        assertEquals(SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED, settlementInput.status());
        assertEquals(1, settlementInputs.size());
        assertEquals(SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED, settlementInputs.get(0).status());
        assertFalse(settlementInput.toString().contains("settled"));
        assertFalse(settlementInput.toString().contains("posted"));
        assertFalse(settlementInput.toString().contains("balance"));
        assertNotNull(tradeFill.tradePriceRule());
    }
}
