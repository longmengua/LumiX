package com.lumix.trading.core.futures.sandbox.funding;

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
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPrice;
import com.lumix.trading.core.futures.sandbox.market.FuturesSandboxMockMarkPriceSource;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 funding 只做 deterministic preview，並明確保留 position 視角的收付款符號。
 */
class FuturesSandboxFundingPreviewCalculatorTest {

    private final FuturesSandboxFundingPreviewCalculator calculator = new FuturesSandboxFundingPreviewCalculator();

    /**
     * 確認正 rate 時 LONG 應付而 SHORT 應收，且結果不修改輸入 position。
     */
    @Test
    void previewsPositiveRateWithOppositeLongAndShortDirections() {
        FuturesPosition longPosition = position("long", FuturesPositionSide.LONG);
        FuturesPosition shortPosition = position("short", FuturesPositionSide.SHORT);

        FuturesSandboxFundingPreview longPreview = calculator.preview(request(longPosition, "0.01", "2026-07-18T03:00:00Z"));
        FuturesSandboxFundingPreview shortPreview = calculator.preview(request(shortPosition, "0.01", "2026-07-18T03:00:00Z"));

        assertPreview(longPreview, "-2.2", FuturesSandboxFundingDirection.PAY);
        assertPreview(shortPreview, "2.2", FuturesSandboxFundingDirection.RECEIVE);
        assertEquals(new FuturesPositionId("position-long"), longPosition.positionId());
        assertEquals(0, new BigDecimal("2").compareTo(longPosition.quantity().value()));
    }

    /**
     * 確認負 rate 會反轉經濟方向，零 rate 則保留零金額且不宣告付款或收款。
     */
    @Test
    void reversesDirectionForNegativeRateAndKeepsZeroRateNeutral() {
        FuturesSandboxFundingPreview negativeRate = calculator.preview(request(
                position("negative", FuturesPositionSide.LONG), "-0.01", "2026-07-18T03:00:00Z"
        ));
        FuturesSandboxFundingPreview zeroRate = calculator.preview(request(
                position("zero", FuturesPositionSide.SHORT), "0", "2026-07-18T03:00:00Z"
        ));

        assertPreview(negativeRate, "2.2", FuturesSandboxFundingDirection.RECEIVE);
        assertPreview(zeroRate, "0", FuturesSandboxFundingDirection.NONE);
    }

    /**
     * 確認 preview 輸入先鎖定帳戶、market 與時間順序，避免錯誤快照被拿去試算。
     */
    @Test
    void rejectsAccountMarketAndTimeBoundaryViolations() {
        FuturesPosition position = position("guard", FuturesPositionSide.LONG);
        FuturesAccount anotherAccount = FuturesAccount.open(
                new AccountId("another-account"),
                new UserId("another-user"),
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-18T00:00:00Z")
        );

        IllegalArgumentException accountException = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxFundingPreviewRequest(
                        anotherAccount, position, markPrice("BTC-USDT"), new FuturesSandboxFundingRate(new BigDecimal("0.01")),
                        Instant.parse("2026-07-18T03:00:00Z")
                )
        );
        IllegalArgumentException marketException = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxFundingPreviewRequest(
                        account(position.futuresAccountId()), position, markPrice("ETH-USDT"),
                        new FuturesSandboxFundingRate(new BigDecimal("0.01")), Instant.parse("2026-07-18T03:00:00Z")
                )
        );
        IllegalArgumentException timeException = assertThrows(IllegalArgumentException.class, () ->
                new FuturesSandboxFundingPreviewRequest(
                        account(position.futuresAccountId()), position, markPrice("BTC-USDT"),
                        new FuturesSandboxFundingRate(new BigDecimal("0.01")), Instant.parse("2026-07-18T01:59:59Z")
                )
        );

        assertEquals("futuresAccount must own position", accountException.getMessage());
        assertEquals("markPrice market must match position market", marketException.getMessage());
        assertEquals("fundingAt must not be before markPrice publishedAt", timeException.getMessage());
    }

    private static FuturesSandboxFundingPreviewRequest request(
            FuturesPosition position,
            String rate,
            String fundingAt
    ) {
        return new FuturesSandboxFundingPreviewRequest(
                account(position.futuresAccountId()),
                position,
                markPrice("BTC-USDT"),
                new FuturesSandboxFundingRate(new BigDecimal(rate)),
                Instant.parse(fundingAt)
        );
    }

    private static FuturesSandboxMockMarkPrice markPrice(String market) {
        return new FuturesSandboxMockMarkPrice(
                new FuturesMarketSymbol(market),
                new BigDecimal("110"),
                FuturesSandboxMockMarkPriceSource.MANUAL_SANDBOX_INPUT,
                Instant.parse("2026-07-18T02:00:00Z")
        );
    }

    private static FuturesPosition position(String id, FuturesPositionSide side) {
        return FuturesPosition.open(
                new FuturesPositionId("position-" + id),
                new AccountId("futures-account"),
                new FuturesMarketSymbol("BTC-USDT"),
                side,
                new FuturesPositionQuantity(new BigDecimal("2")),
                new FuturesEntryPrice(new BigDecimal("100")),
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

    private static void assertPreview(
            FuturesSandboxFundingPreview preview,
            String expectedAmount,
            FuturesSandboxFundingDirection expectedDirection
    ) {
        assertEquals(0, new BigDecimal(expectedAmount).compareTo(preview.signedFundingAmount().value()));
        assertEquals(expectedDirection, preview.direction());
        assertEquals(new AssetSymbol("USDT"), preview.settlementAsset());
    }
}
