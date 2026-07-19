package com.lumix.trading.core.futures.sandbox.funding.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPosition;
import com.lumix.trading.core.futures.position.FuturesPositionId;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import com.lumix.trading.core.futures.position.FuturesPositionSide;
import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreviewRequest;
import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingRate;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 驗證 funding mock batch 只收斂同一 scenario 的 preview，不做付款或 netting。 */
class FuturesSandboxFundingMockBatchSimulatorTest {
    private final FuturesSandboxFundingMockBatchSimulator simulator = new FuturesSandboxFundingMockBatchSimulator();

    @Test
    void simulatesIndependentPreviewsForOneFundingScenario() {
        var result = simulator.simulate(new FuturesSandboxFundingMockBatchRequest(List.of(request("long", FuturesPositionSide.LONG, "BTC-USDT", "0.01", "02:00:00Z"), request("short", FuturesPositionSide.SHORT, "BTC-USDT", "0.01", "02:00:00Z"))));
        assertEquals(2, result.previews().size());
        assertEquals(-1, result.previews().getFirst().signedFundingAmount().value().signum());
        assertEquals(1, result.previews().get(1).signedFundingAmount().value().signum());
    }

    @Test
    void rejectsMixedScenarioAndDuplicatePositionInputs() {
        assertEquals("all previews must use the same market", assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxFundingMockBatchRequest(List.of(request("a", FuturesPositionSide.LONG, "BTC-USDT", "0.01", "02:00:00Z"), request("b", FuturesPositionSide.SHORT, "ETH-USDT", "0.01", "02:00:00Z")))).getMessage());
        assertEquals("all previews must use the same fundingRate", assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxFundingMockBatchRequest(List.of(request("a", FuturesPositionSide.LONG, "BTC-USDT", "0.01", "02:00:00Z"), request("b", FuturesPositionSide.SHORT, "BTC-USDT", "0.02", "02:00:00Z")))).getMessage());
        assertEquals("all previews must use the same fundingAt", assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxFundingMockBatchRequest(List.of(request("a", FuturesPositionSide.LONG, "BTC-USDT", "0.01", "02:00:00Z"), request("b", FuturesPositionSide.SHORT, "BTC-USDT", "0.01", "03:00:00Z")))).getMessage());
        assertEquals("previews must not contain duplicate positionId", assertThrows(IllegalArgumentException.class, () -> new FuturesSandboxFundingMockBatchRequest(List.of(request("a", FuturesPositionSide.LONG, "BTC-USDT", "0.01", "02:00:00Z"), request("a", FuturesPositionSide.SHORT, "BTC-USDT", "0.01", "02:00:00Z")))).getMessage());
    }

    private static FuturesSandboxFundingPreviewRequest request(String id, FuturesPositionSide side, String market, String rate, String time) {
        AccountId accountId = new AccountId("account-" + id);
        return new FuturesSandboxFundingPreviewRequest(FuturesAccount.open(accountId, new UserId("user-" + id), new AssetSymbol("USDT"), Instant.parse("2026-07-20T00:00:00Z")), FuturesPosition.open(new FuturesPositionId("position-" + id), accountId, new FuturesMarketSymbol(market), side, new FuturesPositionQuantity(new BigDecimal("2")), new FuturesEntryPrice(new BigDecimal("100")), Instant.parse("2026-07-20T00:00:00Z")), new FuturesSandboxMockMarkPrice(new FuturesMarketSymbol(market), new BigDecimal("110"), FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT, Instant.parse("2026-07-20T01:00:00Z")), new FuturesSandboxFundingRate(new BigDecimal(rate)), Instant.parse("2026-07-20T" + time));
    }
}
