package com.lumix.trading.core.futures.position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 isolated margin futures position model 只保留 T02 需要的最小不變式。
 */
class FuturesPositionTest {

    /**
     * 確認 LONG position 可以透過 convenience factory 建立成合法開倉資料。
     *
     * 這個 case 必須存在，因為 T02 需要先把最基本的 position direction 與 identity 關係釘死。
     */
    @Test
    void openCreatesValidLongPosition() {
        Instant openedAt = Instant.parse("2026-07-14T03:04:05Z");

        FuturesPosition position = FuturesPosition.open(
                new FuturesPositionId("pos-long-001"),
                new AccountId("futures-acct-101"),
                new FuturesMarketSymbol(" btc-usdt "),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1.25")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                openedAt
        );

        assertEquals(new FuturesPositionId("pos-long-001"), position.positionId());
        assertEquals(new AccountId("futures-acct-101"), position.futuresAccountId());
        assertEquals(new FuturesMarketSymbol("BTC-USDT"), position.marketSymbol());
        assertEquals(FuturesPositionSide.LONG, position.side());
        assertEquals(new FuturesPositionQuantity(new BigDecimal("1.25")), position.quantity());
        assertEquals(new FuturesEntryPrice(new BigDecimal("65000")), position.entryPrice());
        assertEquals(FuturesPositionStatus.OPEN, position.status());
        assertEquals(openedAt, position.openedAt());
        assertEquals(openedAt, position.updatedAt());
    }

    /**
     * 確認 SHORT position 也能以相同模型表達，且不需要 signed quantity 來代替方向。
     *
     * 這個 case 必須存在，因為 direction 必須是顯式 enum，而不是靠正負號推導。
     */
    @Test
    void openCreatesValidShortPosition() {
        Instant openedAt = Instant.parse("2026-07-14T04:05:06Z");

        FuturesPosition position = FuturesPosition.open(
                new FuturesPositionId("pos-short-001"),
                new AccountId("futures-acct-102"),
                new FuturesMarketSymbol("eth-usdt"),
                FuturesPositionSide.SHORT,
                new FuturesPositionQuantity(new BigDecimal("2.500")),
                new FuturesEntryPrice(new BigDecimal("3200.50")),
                openedAt
        );

        assertEquals(FuturesPositionSide.SHORT, position.side());
        assertTrue(position.side().isShort());
        assertFalse(position.side().isLong());
        assertEquals(openedAt, position.openedAt());
        assertEquals(openedAt, position.updatedAt());
    }

    /**
     * 確認 canonical constructor 可以直接承接 rehydration 用的 OPEN position。
     *
     * 這個 case 必須存在，因為 canonical constructor 是驗證邊界，不是被 convenience factory 取代的唯一入口。
     */
    @Test
    void canonicalConstructorAllowsDirectOpenPositionInstantiation() {
        Instant openedAt = Instant.parse("2026-07-14T05:06:07Z");
        Instant updatedAt = Instant.parse("2026-07-14T05:06:08Z");

        FuturesPosition position = new FuturesPosition(
                new FuturesPositionId("pos-closed-001"),
                new AccountId("futures-acct-103"),
                new FuturesMarketSymbol(" sol-usdt "),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("3")),
                new FuturesEntryPrice(new BigDecimal("150.75")),
                FuturesPositionStatus.OPEN,
                openedAt,
                updatedAt
        );

        assertEquals(new FuturesMarketSymbol("SOL-USDT"), position.marketSymbol());
        assertEquals(FuturesPositionStatus.OPEN, position.status());
        assertEquals(updatedAt, position.updatedAt());
    }

    /**
     * 確認必要欄位與時間欄位都不能被繞過。
     *
     * 這個 case 必須存在，因為 position identity、account identity、symbol、side、quantity、entry price
     * 與 openedAt 都是 isolated margin position 的最小必備前提。
     */
    @Test
    void constructorRejectsNullMandatoryFields() {
        Instant openedAt = Instant.parse("2026-07-14T06:07:08Z");

        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                null,
                new AccountId("futures-acct-201"),
                null,
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-account"),
                null,
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-symbol"),
                new AccountId("futures-acct-201"),
                null,
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-side"),
                new AccountId("futures-acct-201"),
                new FuturesMarketSymbol("BTC-USDT"),
                null,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-qty"),
                new AccountId("futures-acct-201"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                null,
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-entry"),
                new AccountId("futures-acct-201"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                null,
                FuturesPositionStatus.OPEN,
                openedAt,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-opened"),
                new AccountId("futures-acct-201"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                null,
                openedAt
        ));
        assertThrows(NullPointerException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-null-updated"),
                new AccountId("futures-acct-201"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                null
        ));
    }

    /**
     * 確認數量與 entry price 不接受 0 或負值。
     *
     * 這個 case 必須存在，因為 position magnitude 與 entry price 是後續 risk 與 valuation 的基礎，
     * 一旦允許 0 或負值，就會讓 Phase 17 的 core model 失去可信度。
     */
    @Test
    void constructorRejectsInvalidMagnitudeValues() {
        assertThrows(IllegalArgumentException.class, () -> new FuturesPositionQuantity(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new FuturesPositionQuantity(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new FuturesEntryPrice(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new FuturesEntryPrice(new BigDecimal("-0.01")));
    }

    /**
     * 確認時間倒序會被直接拒絕。
     *
     * 這個 case 必須存在，因為 openedAt 與 updatedAt 是審計與回放的最小生命週期依據。
     */
    @Test
    void constructorRejectsTimestampRegression() {
        Instant openedAt = Instant.parse("2026-07-14T07:08:09Z");
        Instant updatedAt = Instant.parse("2026-07-14T07:08:08Z");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new FuturesPosition(
                new FuturesPositionId("pos-time-regress"),
                new AccountId("futures-acct-202"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesPositionSide.LONG,
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("65000")),
                FuturesPositionStatus.OPEN,
                openedAt,
                updatedAt
        ));

        assertEquals("updatedAt must not be before openedAt", exception.getMessage());
    }

    /**
     * 確認方向只允許 LONG / SHORT，且模型沒有可用來表達 cross margin 的欄位。
     *
     * 這個 case 必須存在，因為 T02 的 isolated boundary 不能偷渡 cross margin pooling。
     */
    @Test
    void modelSupportsOnlyExplicitLongShortAndNoCrossMarginField() {
        assertEquals(List.of(FuturesPositionSide.LONG, FuturesPositionSide.SHORT), List.of(FuturesPositionSide.values()));
        assertTrue(FuturesPositionSide.LONG.isLong());
        assertTrue(FuturesPositionSide.SHORT.isShort());
        assertEquals(List.of(FuturesPositionStatus.OPEN), List.of(FuturesPositionStatus.values()));

        List<String> recordComponents = Arrays.stream(FuturesPosition.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(
                List.of(
                        "positionId",
                        "futuresAccountId",
                        "marketSymbol",
                        "side",
                        "quantity",
                        "entryPrice",
                        "status",
                        "openedAt",
                        "updatedAt"
                ),
                recordComponents
        );
        assertFalse(recordComponents.contains("crossMargin"));
        assertFalse(recordComponents.contains("marginMode"));
    }
}
