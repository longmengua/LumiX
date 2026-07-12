package com.lumix.trading.core.spot.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.trading.core.spot.matching.SpotSandboxSettlementInput;
import com.lumix.trading.core.spot.matching.SpotSandboxTradeFillStatus;
import com.lumix.trading.core.spot.matching.SpotSandboxTradePriceRule;
import com.lumix.ledger.domain.LedgerDirection;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 Spot sandbox settlement runtime gate 只停在 sandbox plan，不會偷接成正式 settlement runtime。
 */
class SpotSandboxSettlementRuntimeGateTest {

    private final SpotSandboxSettlementRuntimeBoundary boundary = new SpotSandboxSettlementRuntimeBoundary();

    /**
     * 確認 valid settlement input 只會產生 sandbox plan，且不會執行 ledger posting / balance refresh / reservation commit。
     *
     * 這個 case 必須存在，因為 settlement gate 的唯一合法結果就是 plan，不是正式資金流完成。
     */
    @Test
    void validSettlementInputProducesPlannedResultOnly() {
        SpotSandboxSettlementInput settlementInput = settlementInput(
                "sandbox-trade-1",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("50000.00"),
                new BigDecimal("0.25"),
                new BigDecimal("12500.0000"),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        );

        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput);

        assertEquals(SpotSandboxSettlementRuntimeDecision.PLANNED, result.decision());
        assertTrue(result.plan() != null);
        assertEquals(SpotSandboxSettlementPlanStatus.PLANNED_FOR_SANDBOX, result.plan().status());
        assertEquals("sandbox-settlement-request-sandbox-trade-1", result.plan().ledgerPostingCommandCandidate().requestId());
        assertEquals("sandbox-settlement-idempotency-sandbox-trade-1", result.plan().ledgerPostingCommandCandidate().idempotencyKey());
        assertFalse(result.plan().ledgerPostingCommandCandidate().requestId().equals(
                result.plan().ledgerPostingCommandCandidate().idempotencyKey()
        ));
        assertEquals("BTC", result.plan().baseAsset());
        assertEquals("USDT", result.plan().quoteAsset());
        assertEquals(new BigDecimal("50000.00"), result.plan().price());
        assertEquals(new BigDecimal("0.25"), result.plan().quantity());
        assertEquals(new BigDecimal("12500.0000"), result.plan().quoteAmount());
        assertTrue(result.plan().flags().contains(SpotSandboxSettlementPlanFlag.LEDGER_NOT_POSTED));
        assertTrue(result.plan().flags().contains(SpotSandboxSettlementPlanFlag.BALANCE_NOT_UPDATED));
        assertTrue(result.plan().flags().contains(SpotSandboxSettlementPlanFlag.RESERVATION_NOT_COMMITTED));
        assertTrue(result.plan().flags().contains(SpotSandboxSettlementPlanFlag.RECONCILIATION_NOT_COMPLETED));
        assertEquals(4, result.plan().movements().size());
        assertEquals(SpotSandboxAssetMovementType.RECEIVE_BASE, result.plan().movements().get(0).movementType());
        assertEquals(SpotSandboxAssetMovementType.PAY_QUOTE, result.plan().movements().get(1).movementType());
        assertEquals(SpotSandboxAssetMovementType.PAY_BASE, result.plan().movements().get(2).movementType());
        assertEquals(SpotSandboxAssetMovementType.RECEIVE_QUOTE, result.plan().movements().get(3).movementType());
        assertEquals(LedgerDirection.CREDIT, result.plan().ledgerPostingCommandCandidate().lines().get(0).direction());
        assertEquals(LedgerDirection.DEBIT, result.plan().ledgerPostingCommandCandidate().lines().get(1).direction());
        assertEquals(LedgerDirection.DEBIT, result.plan().ledgerPostingCommandCandidate().lines().get(2).direction());
        assertEquals(LedgerDirection.CREDIT, result.plan().ledgerPostingCommandCandidate().lines().get(3).direction());
        assertFalse(result.message().contains("posted"));
        assertFalse(result.message().contains("settled"));
        assertFalse(result.message().contains("balance updated"));
        assertFalse(result.message().contains("reservation committed"));
    }

    /**
     * 確認 malformed marketSymbol 必須被拒絕，避免把 settlement plan 建立在無法解析的 market 上。
     *
     * 這個 case 必須存在，因為 settlement plan 一旦市場名稱不合法，就無法安全拆分 base / quote asset。
     */
    @Test
    void malformedMarketSymbolIsRejected() {
        assertRejectedMarketSymbol("sandbox-trade-2", "BTC-USDT-", "INVALID_MARKET_SYMBOL");
        assertRejectedMarketSymbol("sandbox-trade-3", "BTC-", "INVALID_MARKET_SYMBOL");
        assertRejectedMarketSymbol("sandbox-trade-4", "-USDT", "INVALID_MARKET_SYMBOL");
        assertRejectedMarketSymbol("sandbox-trade-5", "BTC--USDT", "INVALID_MARKET_SYMBOL");
        assertRejectedMarketSymbol("sandbox-trade-6", "BTCUSDT", "INVALID_MARKET_SYMBOL");
    }

    /**
     * 確認合法 marketSymbol 仍能正確拆出 base / quote asset。
     *
     * 這個 case 必須存在，因為 settlement plan 依賴 base / quote asset 正確分拆才能產生 movement 與 ledger candidate。
     */
    @Test
    void validMarketSymbolParsesBaseAndQuoteAssets() {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput(
                "sandbox-trade-7",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        ));

        assertEquals(SpotSandboxSettlementRuntimeDecision.PLANNED, result.decision());
        assertEquals("BTC", result.plan().baseAsset());
        assertEquals("USDT", result.plan().quoteAsset());
    }

    /**
     * 確認 wrong input status 必須被拒絕。
     *
     * 這個 case 必須存在，因為 settlement runtime gate 只接受尚未開始結算的 input。
     */
    @Test
    void wrongInputStatusIsRejected() {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput(
                "sandbox-trade-3",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                SpotSandboxTradeFillStatus.CREATED_FOR_SANDBOX
        ));

        assertEquals(SpotSandboxSettlementRuntimeDecision.REJECTED, result.decision());
        assertEquals("SETTLEMENT_STATUS_NOT_ALLOWED", result.rejectionReason());
    }

    /**
     * 確認 missing idempotencyKey 必須被拒絕。
     *
     * 這個 case 必須存在，因為 settlement runtime gate 不能在沒有 duplicate-prevention contract 的情況下往下走。
     */
    @Test
    void missingIdempotencyKeyIsRejected() {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(new SpotSandboxSettlementInput(
                "sandbox-settlement-request-sandbox-trade-5",
                null,
                "sandbox-trade-5",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                Instant.parse("2026-07-12T12:00:00Z"),
                SpotSandboxTradePriceRule.MAKER_PRICE,
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        ));

        assertEquals(SpotSandboxSettlementRuntimeDecision.REJECTED, result.decision());
        assertEquals("MISSING_IDEMPOTENCY_KEY", result.rejectionReason());
    }

    /**
     * 確認 settlement plan 不會宣稱已經 persisted / settled / posted / balance updated / reservation committed。
     *
     * 這個 case 必須存在，因為 settlement gate 的輸出只能是 plan candidate，不能冒充正式完成。
     */
    @Test
    void resultDoesNotClaimCompletedRuntime() {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput(
                "sandbox-trade-4",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        ));

        assertFalse(result.toString().contains("persisted"));
        assertFalse(result.toString().contains("settled"));
        assertFalse(result.toString().contains("posted"));
        assertFalse(result.toString().contains("balance updated"));
        assertFalse(result.toString().contains("reservation committed"));
    }

    /**
     * 確認 ledger candidate 四腿分錄與金額語意正確。
     *
     * 這個 case 必須存在，因為 settlement runtime gate 產生的 candidate 之後會接到 ledger posting review。
     */
    @Test
    void ledgerCandidateHasFourValidLegs() {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput(
                "sandbox-trade-8",
                "BTC-USDT",
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("50000.00"),
                new BigDecimal("0.25"),
                new BigDecimal("12500.0000"),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        ));

        assertEquals(4, result.plan().ledgerPostingCommandCandidate().lines().size());
        assertEquals(LedgerDirection.CREDIT, result.plan().ledgerPostingCommandCandidate().lines().get(0).direction());
        assertEquals(LedgerDirection.DEBIT, result.plan().ledgerPostingCommandCandidate().lines().get(1).direction());
        assertEquals(LedgerDirection.DEBIT, result.plan().ledgerPostingCommandCandidate().lines().get(2).direction());
        assertEquals(LedgerDirection.CREDIT, result.plan().ledgerPostingCommandCandidate().lines().get(3).direction());
        assertTrue(result.plan().ledgerPostingCommandCandidate().lines().stream().allMatch(line -> line.amount().compareTo(BigDecimal.ZERO) > 0));
    }

    private static void assertRejectedMarketSymbol(String sandboxTradeId, String marketSymbol, String expectedReason) {
        SpotSandboxSettlementRuntimeResult result = boundary.plan(settlementInput(
                sandboxTradeId,
                marketSymbol,
                "buy-order-1",
                "sell-order-1",
                "buy-acct",
                "sell-acct",
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                SpotSandboxTradeFillStatus.SETTLEMENT_NOT_STARTED
        ));

        assertEquals(SpotSandboxSettlementRuntimeDecision.REJECTED, result.decision());
        assertEquals(expectedReason, result.rejectionReason());
    }

    private static SpotSandboxSettlementInput settlementInput(
            String sandboxTradeId,
            String marketSymbol,
            String buyOrderId,
            String sellOrderId,
            String buyAccountId,
            String sellAccountId,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal quoteAmount,
            SpotSandboxTradeFillStatus status
    ) {
        return new SpotSandboxSettlementInput(
                "sandbox-settlement-request-" + sandboxTradeId,
                "sandbox-settlement-idempotency-" + sandboxTradeId,
                sandboxTradeId,
                marketSymbol,
                buyOrderId,
                sellOrderId,
                buyAccountId,
                sellAccountId,
                price,
                quantity,
                quoteAmount,
                Instant.parse("2026-07-12T12:00:00Z"),
                SpotSandboxTradePriceRule.MAKER_PRICE,
                status
        );
    }
}
