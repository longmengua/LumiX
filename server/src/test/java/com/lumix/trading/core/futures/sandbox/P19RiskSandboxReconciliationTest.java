package com.lumix.trading.core.futures.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingPreviewRequest;
import com.lumix.trading.core.futures.sandbox.funding.FuturesSandboxFundingRate;
import com.lumix.trading.core.futures.sandbox.funding.mock.FuturesSandboxFundingMockBatchRequest;
import com.lumix.trading.core.futures.sandbox.funding.mock.FuturesSandboxFundingMockBatchSimulator;
import com.lumix.trading.core.futures.sandbox.insurance.FuturesSandboxInsuranceFundPlaceholder;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 驗證 P19 sandbox 的 funding mock 與 insurance placeholder 可被重放檢查，但不會產生資金異動。 */
class P19RiskSandboxReconciliationTest {
    @Test
    void symmetricFundingPreviewConservesMockAmountAndLeavesInsuranceSnapshotUntouched() {
        FuturesSandboxInsuranceFundPlaceholder insurance = new FuturesSandboxInsuranceFundPlaceholder(new AssetSymbol("USDT"), new MoneyAmount(new BigDecimal("50")), Instant.parse("2026-07-20T00:00:00Z"));
        var previews = new FuturesSandboxFundingMockBatchSimulator().simulate(new FuturesSandboxFundingMockBatchRequest(List.of(request("long", FuturesPositionSide.LONG), request("short", FuturesPositionSide.SHORT)))).previews();
        BigDecimal net = previews.stream().map(preview -> preview.signedFundingAmount().value()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, net.compareTo(BigDecimal.ZERO));
        assertEquals(0, insurance.simulatedAmount().value().compareTo(new BigDecimal("50")));
    }
    private static FuturesSandboxFundingPreviewRequest request(String id, FuturesPositionSide side) {
        AccountId accountId = new AccountId("account-" + id); FuturesMarketSymbol market = new FuturesMarketSymbol("BTC-USDT");
        return new FuturesSandboxFundingPreviewRequest(FuturesAccount.open(accountId, new UserId("user-" + id), new AssetSymbol("USDT"), Instant.parse("2026-07-20T00:00:00Z")), FuturesPosition.open(new FuturesPositionId("position-" + id), accountId, market, side, new FuturesPositionQuantity(new BigDecimal("2")), new FuturesEntryPrice(new BigDecimal("100")), Instant.parse("2026-07-20T00:00:00Z")), new FuturesSandboxMockMarkPrice(market, new BigDecimal("110"), FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT, Instant.parse("2026-07-20T01:00:00Z")), new FuturesSandboxFundingRate(new BigDecimal("0.01")), Instant.parse("2026-07-20T02:00:00Z"));
    }
}
