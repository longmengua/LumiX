package com.lumix.trading.core.futures.sandbox.liquidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 liquidation simulator 只回傳明確輸入下的數學結論，不改變 position 或資金狀態。
 */
class FuturesSandboxLiquidationSimulatorTest {

    private final FuturesSandboxLiquidationSimulator simulator = new FuturesSandboxLiquidationSimulator();

    /**
     * 確認獲利 LONG 的 simulated equity 高於 maintenance margin 時維持非強平結論。
     */
    @Test
    void keepsProfitableLongAboveMaintenanceMargin() {
        FuturesPosition position = position("long", FuturesPositionSide.LONG, "2", "100");

        FuturesSandboxLiquidationSimulationResult result = simulator.simulate(request(position, "110", "10", "0.1"));

        assertEquals(FuturesSandboxLiquidationSimulationDecision.NOT_LIQUIDATABLE, result.decision());
        assertEquals(FuturesSandboxLiquidationSimulationReason.EQUITY_ABOVE_MAINTENANCE_MARGIN, result.reason());
        assertMoneyEquals("30", result.simulatedEquity());
        assertMoneyEquals("22", result.simulatedMaintenanceMargin());
        assertEquals(new FuturesPositionId("position-long"), position.positionId());
    }

    /**
     * 確認虧損 SHORT 與剛好等於 maintenance margin 都會得到保守的 simulated-liquidation 結論。
     */
    @Test
    void simulatesLiquidationForLossAndAtMaintenanceBoundary() {
        FuturesSandboxLiquidationSimulationResult losingShort = simulator.simulate(request(
                position("short", FuturesPositionSide.SHORT, "2", "100"), "110", "10", "0.1"
        ));
        FuturesSandboxLiquidationSimulationResult atBoundary = simulator.simulate(request(
                position("boundary", FuturesPositionSide.LONG, "1", "100"), "100", "10", "0.1"
        ));

        assertLiquidation(losingShort, "-10", "22");
        assertLiquidation(atBoundary, "10", "10");
    }

    /**
     * 確認 simulation 必須鎖定 account/market，並拒絕負 collateral 與無效 maintenance rate。
     */
    @Test
    void rejectsOwnershipMarketCollateralAndRateBoundaryViolations() {
        FuturesPosition position = position("guard", FuturesPositionSide.LONG, "1", "100");
        FuturesAccount anotherAccount = account(new AccountId("another-account"));

        IllegalArgumentException ownership = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxLiquidationSimulationRequest(
                        anotherAccount, position, markPrice("BTC-USDT", "100"), collateral("10"), rate("0.1")
                )
        );
        IllegalArgumentException market = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxLiquidationSimulationRequest(
                        account(position.futuresAccountId()), position, markPrice("ETH-USDT", "100"), collateral("10"), rate("0.1")
                )
        );
        IllegalArgumentException collateral = assertThrows(IllegalArgumentException.class, () -> collateral("-0.01"));
        IllegalArgumentException rate = assertThrows(IllegalArgumentException.class, () -> rate("1"));

        assertEquals("futuresAccount must own position", ownership.getMessage());
        assertEquals("markPrice market must match position market", market.getMessage());
        assertEquals("simulatedCollateral must not be negative", collateral.getMessage());
        assertEquals("maintenanceMarginRate must be greater than zero and less than one", rate.getMessage());
    }

    private static FuturesSandboxLiquidationSimulationRequest request(
            FuturesPosition position, String markPrice, String collateral, String rate
    ) {
        return new FuturesSandboxLiquidationSimulationRequest(
                account(position.futuresAccountId()), position, markPrice("BTC-USDT", markPrice), collateral(collateral), rate(rate)
        );
    }

    private static FuturesPosition position(String id, FuturesPositionSide side, String quantity, String entryPrice) {
        return FuturesPosition.open(new FuturesPositionId("position-" + id), new AccountId("futures-account"),
                new FuturesMarketSymbol("BTC-USDT"), side, new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(entryPrice)), Instant.parse("2026-07-20T00:00:00Z"));
    }

    private static FuturesAccount account(AccountId accountId) {
        return FuturesAccount.open(accountId, new UserId("futures-user"), new AssetSymbol("USDT"), Instant.parse("2026-07-20T00:00:00Z"));
    }

    private static FuturesSandboxMockMarkPrice markPrice(String market, String price) {
        return new FuturesSandboxMockMarkPrice(new FuturesMarketSymbol(market), new BigDecimal(price),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT, Instant.parse("2026-07-20T00:01:00Z"));
    }

    private static FuturesSandboxSimulatedCollateral collateral(String amount) {
        return new FuturesSandboxSimulatedCollateral(new MoneyAmount(new BigDecimal(amount)));
    }

    private static FuturesSandboxMaintenanceMarginRate rate(String value) {
        return new FuturesSandboxMaintenanceMarginRate(new BigDecimal(value));
    }

    private static void assertLiquidation(FuturesSandboxLiquidationSimulationResult result, String equity, String maintenance) {
        assertEquals(FuturesSandboxLiquidationSimulationDecision.LIQUIDATION_SIMULATED, result.decision());
        assertEquals(FuturesSandboxLiquidationSimulationReason.EQUITY_AT_OR_BELOW_MAINTENANCE_MARGIN, result.reason());
        assertMoneyEquals(equity, result.simulatedEquity());
        assertMoneyEquals(maintenance, result.simulatedMaintenanceMargin());
    }

    private static void assertMoneyEquals(String expected, MoneyAmount actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.value()));
    }
}
