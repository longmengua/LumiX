package com.lumix.trading.core.futures.margin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 驗證 margin check request 只能承接完整且不可變的 isolated margin 輸入快照。
 */
class IsolatedMarginCheckRequestTest {

    /**
     * 確認合法 request 可以保留既有 futures model 與可用保證金快照。
     *
     * 這個 case 必須存在，因為 T04 的 gate 只能依賴明確輸入快照，不能偷偷去查外部狀態。
     */
    @Test
    void constructorAcceptsValidRequest() {
        IsolatedMarginCheckRequest request = validRequest(new MoneyAmount(new BigDecimal("1250.50")));

        assertEquals(new FuturesMarketSymbol("BTC-USDT"), request.marketSymbol());
        assertEquals(new AssetSymbol("USDT"), request.availableMarginAsset());
        assertEquals(new MoneyAmount(new BigDecimal("1250.50")), request.availableMargin());
    }

    /**
     * 確認必要欄位不可為 null。
     *
     * 這個 case 必須存在，因為 request 是 deterministic gate 的唯一輸入邊界，任何缺欄位都不應變成半成品。
     */
    @Test
    void constructorRejectsNullMandatoryFields() {
        FuturesAccount account = sampleAccount();
        IsolatedLeverageConfig leverageConfig = sampleLeverageConfig();
        FuturesMarketSymbol marketSymbol = new FuturesMarketSymbol("BTC-USDT");
        FuturesPositionQuantity quantity = new FuturesPositionQuantity(new BigDecimal("0.5"));
        FuturesEntryPrice entryPrice = new FuturesEntryPrice(new BigDecimal("50000"));
        AssetSymbol asset = new AssetSymbol("USDT");
        MoneyAmount availableMargin = new MoneyAmount(new BigDecimal("100"));

        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(null, leverageConfig, marketSymbol, quantity, entryPrice, asset, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, null, marketSymbol, quantity, entryPrice, asset, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, leverageConfig, null, quantity, entryPrice, asset, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, leverageConfig, marketSymbol, null, entryPrice, asset, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, leverageConfig, marketSymbol, quantity, null, asset, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, leverageConfig, marketSymbol, quantity, entryPrice, null, availableMargin));
        assertThrows(NullPointerException.class, () -> new IsolatedMarginCheckRequest(account, leverageConfig, marketSymbol, quantity, entryPrice, asset, null));
    }

    /**
     * 確認 available margin 不接受負數，但接受 0。
     *
     * 這個 case 必須存在，因為 0 可用保證金是合法快照；真正是否足夠由 gate 決定，而不是由 request 偷做業務判斷。
     */
    @Test
    void constructorRejectsNegativeAvailableMarginButAllowsZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validRequest(new MoneyAmount(new BigDecimal("-0.01")))
        );

        IsolatedMarginCheckRequest zeroMarginRequest = validRequest(MoneyAmount.zero());
        assertEquals(MoneyAmount.zero(), zeroMarginRequest.availableMargin());
    }

    /**
     * 確認 direct constructor 不能繞過 invariant。
     *
     * 這個 case 必須存在，因為 canonical constructor 才是 request 的正式驗證邊界。
     */
    @Test
    void directConstructorCannotBypassInvariant() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new IsolatedMarginCheckRequest(
                        sampleAccount(),
                        sampleLeverageConfig(),
                        new FuturesMarketSymbol("BTC-USDT"),
                        new FuturesPositionQuantity(new BigDecimal("0.1")),
                        new FuturesEntryPrice(new BigDecimal("1000")),
                        new AssetSymbol("USDT"),
                        new MoneyAmount(new BigDecimal("-1"))
                )
        );

        assertEquals("availableMargin must not be negative", exception.getMessage());
    }

    private static IsolatedMarginCheckRequest validRequest(MoneyAmount availableMargin) {
        return new IsolatedMarginCheckRequest(
                sampleAccount(),
                sampleLeverageConfig(),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal("0.5")),
                new FuturesEntryPrice(new BigDecimal("50000")),
                new AssetSymbol("USDT"),
                availableMargin
        );
    }

    private static FuturesAccount sampleAccount() {
        return FuturesAccount.open(
                new AccountId("futures-acct-margin-001"),
                new UserId("user-margin-001"),
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-15T01:02:03Z")
        );
    }

    private static IsolatedLeverageConfig sampleLeverageConfig() {
        return IsolatedLeverageConfig.configure(
                new AccountId("futures-acct-margin-001"),
                new FuturesMarketSymbol("BTC-USDT"),
                FuturesLeverage.of(5),
                Instant.parse("2026-07-15T01:02:04Z")
        );
    }
}
