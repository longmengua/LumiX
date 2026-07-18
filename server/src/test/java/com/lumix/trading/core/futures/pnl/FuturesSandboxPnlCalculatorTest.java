package com.lumix.trading.core.futures.pnl;

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
 * 驗證 T04 PnL 只做 deterministic 的價格差額計算，不變更 position 或接入資金 runtime。
 */
class FuturesSandboxPnlCalculatorTest {

    private final FuturesSandboxPnlCalculator calculator = new FuturesSandboxPnlCalculator();

    /**
     * 確認 LONG 與 SHORT 使用相反的價格差額方向，並保留 settlement asset。
     */
    @Test
    void calculatesPositiveAndNegativeUnrealizedPnlForLongAndShort() {
        FuturesSandboxUnrealizedPnlSnapshot longProfit = calculator.calculateUnrealized(unrealizedRequest(
                position("long", FuturesPositionSide.LONG, "2", "100"), "110"
        ));
        FuturesSandboxUnrealizedPnlSnapshot longLoss = calculator.calculateUnrealized(unrealizedRequest(
                position("long-loss", FuturesPositionSide.LONG, "2", "100"), "95"
        ));
        FuturesSandboxUnrealizedPnlSnapshot shortProfit = calculator.calculateUnrealized(unrealizedRequest(
                position("short", FuturesPositionSide.SHORT, "2", "100"), "90"
        ));

        assertMoneyEquals("20", longProfit.unrealizedPnl().value());
        assertMoneyEquals("-10", longLoss.unrealizedPnl().value());
        assertMoneyEquals("20", shortProfit.unrealizedPnl().value());
        assertEquals(new AssetSymbol("USDT"), longProfit.settlementAsset());
        assertEquals(Instant.parse("2026-07-18T02:00:00Z"), longProfit.valuedAt());
    }

    /**
     * 確認 realized preview 可以計算部分 close，但只回傳數學結果，沒有產生 closed position。
     */
    @Test
    void previewsRealizedPnlForPartialCloseWithoutChangingPosition() {
        FuturesPosition longPosition = position("long-close", FuturesPositionSide.LONG, "3", "100");
        FuturesPosition shortPosition = position("short-close", FuturesPositionSide.SHORT, "3", "100");

        FuturesSandboxRealizedPnlPreview longPreview = calculator.previewRealized(realizedRequest(
                longPosition, "110", "1"
        ));
        FuturesSandboxRealizedPnlPreview shortPreview = calculator.previewRealized(realizedRequest(
                shortPosition, "110", "1.5"
        ));

        assertMoneyEquals("10", longPreview.realizedPnl().value());
        assertMoneyEquals("-15", shortPreview.realizedPnl().value());
        assertMoneyEquals("3", longPosition.quantity().value());
        assertEquals(new FuturesPositionId("position-long-close"), longPosition.positionId());
    }

    /**
     * 確認 PnL request 必須屬於同一 futures account，且 close quantity 不能超過目前 position。
     */
    @Test
    void rejectsAccountMismatchAndOversizedCloseQuantity() {
        FuturesPosition position = position("guard", FuturesPositionSide.LONG, "2", "100");
        FuturesAccount otherAccount = FuturesAccount.open(
                new AccountId("other-account"),
                new UserId("other-user"),
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-18T00:00:00Z")
        );

        IllegalArgumentException accountMismatch = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxUnrealizedPnlRequest(
                        otherAccount,
                        position,
                        new FuturesSandboxPnlPrice(new BigDecimal("110")),
                        Instant.parse("2026-07-18T02:00:00Z")
                )
        );
        IllegalArgumentException oversizedClose = assertThrows(IllegalArgumentException.class, () ->
                realizedRequest(position, "110", "2.01")
        );

        assertEquals("futuresAccount must own position", accountMismatch.getMessage());
        assertEquals("closeQuantity must not exceed position quantity", oversizedClose.getMessage());
    }

    private static FuturesSandboxUnrealizedPnlRequest unrealizedRequest(FuturesPosition position, String markPrice) {
        return new FuturesSandboxUnrealizedPnlRequest(
                account(position.futuresAccountId()),
                position,
                new FuturesSandboxPnlPrice(new BigDecimal(markPrice)),
                Instant.parse("2026-07-18T02:00:00Z")
        );
    }

    private static FuturesSandboxRealizedPnlPreviewRequest realizedRequest(
            FuturesPosition position,
            String closePrice,
            String closeQuantity
    ) {
        return new FuturesSandboxRealizedPnlPreviewRequest(
                account(position.futuresAccountId()),
                position,
                new FuturesSandboxPnlPrice(new BigDecimal(closePrice)),
                new FuturesPositionQuantity(new BigDecimal(closeQuantity)),
                Instant.parse("2026-07-18T02:01:00Z")
        );
    }

    private static FuturesPosition position(
            String id,
            FuturesPositionSide side,
            String quantity,
            String entryPrice
    ) {
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

    private static void assertMoneyEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
