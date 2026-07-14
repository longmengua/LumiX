package com.lumix.trading.core.futures.leverage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AccountId;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 isolated leverage config 只保留 account + market 的單一 ownership 邊界。
 */
class IsolatedLeverageConfigTest {

    /**
     * 確認合法 configure path 會建立新的 snapshot，且 createdAt 與 updatedAt 相同。
     *
     * 這個 case 必須存在，因為 configure 是便利入口，不是把 runtime side effect 偷渡進來。
     */
    @Test
    void configureCreatesSnapshotWithEqualTimestamps() {
        Instant configuredAt = Instant.parse("2026-07-14T08:09:10Z");

        IsolatedLeverageConfig config = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-301"),
                new FuturesMarketSymbol("btc-usdt"),
                FuturesLeverage.of(5),
                configuredAt
        );

        assertEquals(new AccountId("futures-acct-301"), config.futuresAccountId());
        assertEquals(new FuturesMarketSymbol("BTC-USDT"), config.marketSymbol());
        assertEquals(FuturesLeverage.of(5), config.leverage());
        assertEquals(configuredAt, config.createdAt());
        assertEquals(configuredAt, config.updatedAt());
    }

    /**
     * 確認 canonical constructor 可以直接承接 rehydration。
     *
     * 這個 case 必須存在，因為 invariant boundary 不能只剩 configure factory。
     */
    @Test
    void canonicalConstructorAllowsRehydration() {
        Instant createdAt = Instant.parse("2026-07-14T09:10:11Z");
        Instant updatedAt = Instant.parse("2026-07-14T09:10:12Z");

        IsolatedLeverageConfig config = new IsolatedLeverageConfig(
                new AccountId("futures-acct-302"),
                new FuturesMarketSymbol("eth-usdt"),
                FuturesLeverage.of(8),
                createdAt,
                updatedAt
        );

        assertEquals(createdAt, config.createdAt());
        assertEquals(updatedAt, config.updatedAt());
    }

    /**
     * 確認必要欄位與時間欄位不能被繞過。
     *
     * 這個 case 必須存在，因為 config 的 ownership 邊界就是 account + market。
     */
    @Test
    void constructorRejectsNullMandatoryFields() {
        Instant ts = Instant.parse("2026-07-14T10:11:12Z");

        assertThrows(NullPointerException.class, () -> new IsolatedLeverageConfig(
                null,
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(3),
                ts,
                ts
        ));
        assertThrows(NullPointerException.class, () -> new IsolatedLeverageConfig(
                new AccountId("futures-acct-303"),
                null,
                FuturesLeverage.of(3),
                ts,
                ts
        ));
        assertThrows(NullPointerException.class, () -> new IsolatedLeverageConfig(
                new AccountId("futures-acct-303"),
                new FuturesMarketSymbol("BTC-USDT"),
                null,
                ts,
                ts
        ));
        assertThrows(NullPointerException.class, () -> new IsolatedLeverageConfig(
                new AccountId("futures-acct-303"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(3),
                null,
                ts
        ));
        assertThrows(NullPointerException.class, () -> new IsolatedLeverageConfig(
                new AccountId("futures-acct-303"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(3),
                ts,
                null
        ));
    }

    /**
     * 確認時間倒序會被直接拒絕。
     */
    @Test
    void constructorRejectsTimestampRegression() {
        Instant createdAt = Instant.parse("2026-07-14T10:11:12Z");
        Instant updatedAt = Instant.parse("2026-07-14T10:11:11Z");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new IsolatedLeverageConfig(
                new AccountId("futures-acct-304"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(3),
                createdAt,
                updatedAt
        ));

        assertEquals("updatedAt must not be before createdAt", exception.getMessage());
    }

    /**
     * 確認不同 account 與不同 market 會形成不同 config snapshot。
     *
     * 這個 case 必須存在，因為 leverage ownership 是 account + market 的組合邊界。
     */
    @Test
    void configurationsStayDistinctAcrossAccountAndMarketOwnership() {
        Instant configuredAt = Instant.parse("2026-07-14T11:12:13Z");

        IsolatedLeverageConfig base = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-401"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(10),
                configuredAt
        );
        IsolatedLeverageConfig differentAccount = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-402"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(10),
                configuredAt
        );
        IsolatedLeverageConfig differentMarket = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-401"),
                new FuturesMarketSymbol("ETH-USDT"),
                FuturesLeverage.of(10),
                configuredAt
        );

        assertNotEquals(base, differentAccount);
        assertNotEquals(base, differentMarket);
    }

    /**
     * 確認 reconfigure 會產生新的 immutable snapshot，並保留 account、market 與 createdAt。
     *
     * 這個 case 必須存在，因為 reconfigure 是審計事件，不是原物件原地修改。
     */
    @Test
    void reconfigureCreatesNewSnapshotAndKeepsOwnershipIdentity() {
        Instant configuredAt = Instant.parse("2026-07-14T12:13:14Z");
        Instant changedAt = Instant.parse("2026-07-14T12:13:15Z");

        IsolatedLeverageConfig original = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-501"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(3),
                configuredAt
        );
        IsolatedLeverageConfig updated = original.reconfigure(FuturesLeverage.of(7), changedAt);

        assertEquals(new AccountId("futures-acct-501"), updated.futuresAccountId());
        assertEquals(new FuturesMarketSymbol("BTC-USDT"), updated.marketSymbol());
        assertEquals(configuredAt, updated.createdAt());
        assertEquals(FuturesLeverage.of(7), updated.leverage());
        assertEquals(changedAt, updated.updatedAt());
        assertEquals(FuturesLeverage.of(3), original.leverage());
        assertEquals(configuredAt, original.createdAt());
        assertEquals(configuredAt, original.updatedAt());
    }

    /**
     * 確認 reconfigure 也允許相同 leverage，但仍會建立新的 snapshot 並更新時間。
     *
     * 這個 case 必須存在，因為「重設同倍率」仍然是可審計事件，不應被當成 no-op 偷吃掉。
     */
    @Test
    void reconfigureWithSameLeverageStillCreatesNewSnapshot() {
        Instant configuredAt = Instant.parse("2026-07-14T13:14:15Z");
        Instant changedAt = Instant.parse("2026-07-14T13:14:16Z");

        IsolatedLeverageConfig original = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-601"),
                new FuturesMarketSymbol("SOL-USDT"),
                FuturesLeverage.of(4),
                configuredAt
        );
        IsolatedLeverageConfig sameMultiplierSnapshot = original.reconfigure(FuturesLeverage.of(4), changedAt);

        assertEquals(FuturesLeverage.of(4), sameMultiplierSnapshot.leverage());
        assertEquals(changedAt, sameMultiplierSnapshot.updatedAt());
        assertNotEquals(original, sameMultiplierSnapshot);
    }

    /**
     * 確認 reconfigure 不接受空值與倒序時間。
     */
    @Test
    void reconfigureRejectsNullValuesAndTimestampRegression() {
        Instant configuredAt = Instant.parse("2026-07-14T14:15:16Z");
        Instant changedAt = Instant.parse("2026-07-14T14:15:17Z");
        IsolatedLeverageConfig original = IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-701"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(2),
                configuredAt
        );

        assertThrows(NullPointerException.class, () -> original.reconfigure(null, changedAt));
        assertThrows(NullPointerException.class, () -> original.reconfigure(FuturesLeverage.of(3), null));
        assertThrows(IllegalArgumentException.class, () -> original.reconfigure(FuturesLeverage.of(3), configuredAt.minusSeconds(1)));
    }
}
