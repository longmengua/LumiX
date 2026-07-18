package com.lumix.trading.core.futures.sandbox.market;

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
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 T05 mock mark price 只能受限地提供給 T04 valuation，不能跨 market 或改變 position。
 */
class FuturesSandboxMarkPricePnlValuationGateTest {

    private final FuturesSandboxMarkPricePnlValuationGate valuationGate = new FuturesSandboxMarkPricePnlValuationGate();

    /**
     * 確認 gate 沿用 snapshot 的時間與 T04 的 LONG PnL 公式，而不自行讀取時鐘或行情服務。
     */
    @Test
    void valuesPositionFromExplicitMockMarkPrice() {
        FuturesPosition position = position("long", FuturesPositionSide.LONG, "2", "100");
        FuturesSandboxMockMarkPrice markPrice = markPrice("BTC-USDT", "110", "2026-07-18T02:00:00Z");

        var snapshot = valuationGate.calculateUnrealized(account(position.futuresAccountId()), position, markPrice);

        assertEquals(0, new BigDecimal("20").compareTo(snapshot.unrealizedPnl().value()));
        assertEquals(markPrice.publishedAt(), snapshot.valuedAt());
        assertEquals(new AssetSymbol("USDT"), snapshot.settlementAsset());
        assertEquals(new FuturesPositionId("position-long"), position.positionId());
        assertEquals(0, new BigDecimal("2").compareTo(position.quantity().value()));
    }

    /**
     * 確認價格快照必須對應 position market，避免將單一 mock 值誤套用到其他合約。
     */
    @Test
    void rejectsMockMarkPriceFromAnotherMarket() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                valuationGate.calculateUnrealized(
                        account(new AccountId("futures-account")),
                        position("guard", FuturesPositionSide.SHORT, "1", "100"),
                        markPrice("ETH-USDT", "110", "2026-07-18T02:00:00Z")
                )
        );

        assertEquals("markPrice market must match position market", exception.getMessage());
    }

    private static FuturesSandboxMockMarkPrice markPrice(String market, String price, String publishedAt) {
        return new FuturesSandboxMockMarkPrice(
                new FuturesMarketSymbol(market),
                new BigDecimal(price),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                Instant.parse(publishedAt)
        );
    }

    private static FuturesPosition position(String id, FuturesPositionSide side, String quantity, String entryPrice) {
        return FuturesPosition.open(
                new FuturesPositionId("position-" + id),
                new AccountId("futures-account"),
                new FuturesMarketSymbol("BTC-USDT"),
                side,
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(entryPrice)),
                Instant.parse("2026-07-18T01:00:00Z")
        );
    }

    private static FuturesAccount account(AccountId accountId) {
        return FuturesAccount.open(
                accountId,
                new UserId("futures-user"),
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-18T00:00:00Z")
        );
    }
}
