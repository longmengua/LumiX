package com.lumix.trading.core.futures.margin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.trading.core.futures.account.FuturesAccount;
import com.lumix.trading.core.futures.account.FuturesMarginMode;
import com.lumix.trading.core.futures.leverage.FuturesLeverage;
import com.lumix.trading.core.futures.leverage.IsolatedLeverageConfig;
import com.lumix.trading.core.futures.position.FuturesEntryPrice;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;
import com.lumix.trading.core.futures.position.FuturesPositionQuantity;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 isolated margin gate 只做 deterministic 的 in-memory 容量比較，不偷渡其他 runtime 行為。
 */
class IsolatedMarginCheckGateTest {

    private final IsolatedMarginCheckGate gate = new IsolatedMarginCheckGate();

    /**
     * 確認 supported > requested 時會通過。
     */
    @Test
    void approvesWhenSupportedNotionalIsGreaterThanRequested() {
        IsolatedMarginCheckResult result = gate.check(request("0.5", "50000", "3000", 10));

        assertEquals(FuturesMarginCheckStatus.APPROVED, result.status());
        assertEquals(FuturesMarginCheckReason.SUFFICIENT_MARGIN, result.reason());
        assertEquals(amount("25000"), result.requestedNotional().orElseThrow());
        assertEquals(amount("30000"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 supported == requested 的等值邊界也必須通過。
     *
     * 這個 case 必須存在，因為 T04 明確要求 equality boundary 使用 compareTo 語意，不能因 scale 或 equals 誤拒絕。
     */
    @Test
    void approvesWhenSupportedNotionalEqualsRequested() {
        IsolatedMarginCheckResult result = gate.check(request("0.5", "50000.0", "2500.000", 10));

        assertEquals(FuturesMarginCheckStatus.APPROVED, result.status());
        assertEquals(FuturesMarginCheckReason.SUFFICIENT_MARGIN, result.reason());
        assertEquals(0, result.marginSupportedNotional().orElseThrow().compareTo(result.requestedNotional().orElseThrow()));
    }

    /**
     * 確認 supported < requested 時會以 INSUFFICIENT_MARGIN 拒絕。
     */
    @Test
    void rejectsWhenSupportedNotionalIsLessThanRequested() {
        IsolatedMarginCheckResult result = gate.check(request("0.5", "50000", "2499.99", 10));

        assertEquals(FuturesMarginCheckStatus.REJECTED, result.status());
        assertEquals(FuturesMarginCheckReason.INSUFFICIENT_MARGIN, result.reason());
        assertEquals(amount("25000"), result.requestedNotional().orElseThrow());
        assertEquals(amount("24999.9"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 0 可用保證金是合法輸入，但只要 requested notional 為正就必須拒絕。
     */
    @Test
    void rejectsPositiveNotionalWhenAvailableMarginIsZero() {
        IsolatedMarginCheckResult result = gate.check(request("0.1", "10000", "0", 3));

        assertEquals(FuturesMarginCheckStatus.REJECTED, result.status());
        assertEquals(FuturesMarginCheckReason.INSUFFICIENT_MARGIN, result.reason());
        assertEquals(amount("1000"), result.requestedNotional().orElseThrow());
        assertEquals(MoneyAmount.zero(), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 1x 與大於 1x 的 leverage 都採相同乘法比較規則。
     */
    @Test
    void evaluatesOneXAndGreaterThanOneXLeverageUsingSameFormula() {
        IsolatedMarginCheckResult oneX = gate.check(request("2", "100", "150", 1));
        IsolatedMarginCheckResult fiveX = gate.check(request("2", "100", "150", 5));

        assertEquals(FuturesMarginCheckReason.INSUFFICIENT_MARGIN, oneX.reason());
        assertEquals(FuturesMarginCheckReason.SUFFICIENT_MARGIN, fiveX.reason());
        assertEquals(amount("200"), oneX.requestedNotional().orElseThrow());
        assertEquals(amount("150"), oneX.marginSupportedNotional().orElseThrow());
        assertEquals(amount("750"), fiveX.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 decimal quantity 與 decimal price 會以 exact multiplication 比較，不做隱性 rounding。
     */
    @Test
    void comparesDecimalQuantityAndPriceExactly() {
        IsolatedMarginCheckResult result = gate.check(request("0.125", "64000.25", "8000.03125", 1));

        assertEquals(FuturesMarginCheckStatus.APPROVED, result.status());
        assertEquals(amount("8000.03125"), result.requestedNotional().orElseThrow());
        assertEquals(amount("8000.03125"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 compareTo semantics 不會被 trailing zero 差異影響。
     */
    @Test
    void compareToSemanticsIgnoreBigDecimalScaleDifferences() {
        IsolatedMarginCheckResult result = gate.check(request("1.000", "10.000", "2.5000", 4));

        assertEquals(FuturesMarginCheckStatus.APPROVED, result.status());
        assertEquals(amount("10"), result.requestedNotional().orElseThrow());
        assertEquals(amount("10"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 ACTIVE account 才能進入真正的 margin comparison。
     */
    @Test
    void activeAccountReachesMarginComparison() {
        IsolatedMarginCheckResult result = gate.check(request("1", "100", "50", 2));

        assertTrue(result.requestedNotional().isPresent());
        assertTrue(result.marginSupportedNotional().isPresent());
    }

    /**
     * 確認非 ACTIVE account 會先被擋下，且不計算 notional。
     */
    @Test
    void rejectsNonActiveAccountBeforeAnyCalculation() {
        FuturesAccount frozenAccount = new FuturesAccount(
                new AccountId("futures-acct-frozen"),
                new UserId("user-frozen"),
                AccountStatus.FROZEN,
                FuturesMarginMode.ISOLATED,
                new AssetSymbol("USDT"),
                Instant.parse("2026-07-15T02:00:00Z"),
                Instant.parse("2026-07-15T02:00:01Z")
        );

        IsolatedMarginCheckResult result = gate.check(new IsolatedMarginCheckRequest(
                frozenAccount,
                sampleLeverageConfig("futures-acct-frozen", "BTC-USDT", 3),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("1000")),
                new AssetSymbol("USDT"),
                amount("500")
        ));

        assertEquals(FuturesMarginCheckReason.ACCOUNT_NOT_ACTIVE, result.reason());
        assertTrue(result.requestedNotional().isEmpty());
    }

    /**
     * 確認 leverage config 不得跨 account 使用。
     */
    @Test
    void rejectsAccountMismatch() {
        IsolatedMarginCheckRequest request = new IsolatedMarginCheckRequest(
                sampleAccount("futures-acct-a", AccountStatus.ACTIVE, "USDT"),
                sampleLeverageConfig("futures-acct-b", "BTC-USDT", 3),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("1000")),
                new AssetSymbol("USDT"),
                amount("500")
        );

        IsolatedMarginCheckResult result = gate.check(request);

        assertEquals(FuturesMarginCheckReason.ACCOUNT_MISMATCH, result.reason());
        assertTrue(result.requestedNotional().isEmpty());
    }

    /**
     * 確認 leverage config 不得跨 market 使用。
     */
    @Test
    void rejectsMarketMismatch() {
        IsolatedMarginCheckRequest request = new IsolatedMarginCheckRequest(
                sampleAccount("futures-acct-a", AccountStatus.ACTIVE, "USDT"),
                sampleLeverageConfig("futures-acct-a", "ETH-USDT", 3),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("1000")),
                new AssetSymbol("USDT"),
                amount("500")
        );

        IsolatedMarginCheckResult result = gate.check(request);

        assertEquals(FuturesMarginCheckReason.MARKET_MISMATCH, result.reason());
        assertTrue(result.marginSupportedNotional().isEmpty());
    }

    /**
     * 確認 settlement margin asset 必須與 futures account settlement asset 相同。
     */
    @Test
    void rejectsSettlementAssetMismatch() {
        IsolatedMarginCheckRequest request = new IsolatedMarginCheckRequest(
                sampleAccount("futures-acct-a", AccountStatus.ACTIVE, "USDT"),
                sampleLeverageConfig("futures-acct-a", "BTC-USDT", 3),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal("1")),
                new FuturesEntryPrice(new BigDecimal("1000")),
                new AssetSymbol("USDC"),
                amount("500")
        );

        IsolatedMarginCheckResult result = gate.check(request);

        assertEquals(FuturesMarginCheckReason.SETTLEMENT_ASSET_MISMATCH, result.reason());
        assertTrue(result.requestedNotional().isEmpty());
    }

    /**
     * 確認比較公式直接使用乘法，不先做除法與 rounding。
     *
     * 這個 case 必須存在，因為若先算 requested / leverage 再用有限精度 rounding，
     * 0.333... 這種邊界值很容易被錯誤放行。
     */
    @Test
    void usesMultiplicationComparisonWithoutDivisionRounding() {
        IsolatedMarginCheckResult result = gate.check(request(
                "1",
                "1",
                "0.3333333333333333333333333333",
                3
        ));

        assertEquals(FuturesMarginCheckStatus.REJECTED, result.status());
        assertEquals(FuturesMarginCheckReason.INSUFFICIENT_MARGIN, result.reason());
        assertEquals(amount("1"), result.requestedNotional().orElseThrow());
        assertEquals(amount("0.9999999999999999999999999999"), result.marginSupportedNotional().orElseThrow());
    }

    /**
     * 確認 gate 與輸入模型都維持 immutable，重複執行結果一致。
     */
    @Test
    void gateIsDeterministicAndDoesNotMutateInputs() {
        IsolatedMarginCheckRequest request = request("0.25", "40000", "4000", 3);
        FuturesAccount originalAccount = request.futuresAccount();
        IsolatedLeverageConfig originalConfig = request.leverageConfig();

        IsolatedMarginCheckResult first = gate.check(request);
        IsolatedMarginCheckResult second = gate.check(request);

        assertEquals(first, second);
        assertEquals(originalAccount, request.futuresAccount());
        assertEquals(originalConfig, request.leverageConfig());
        assertNotSame(first, second);
    }

    /**
     * 確認 gate 只回傳 margin result，不會建立 FuturesPosition、order 或 trade 類型。
     *
     * 這個 case 必須存在，因為 T04 只允許 capacity gate，不能偷渡後續 runtime artefact。
     */
    @Test
    void gateResultDoesNotExposePositionOrderOrTradeArtifacts() {
        List<Class<?>> componentTypes = Arrays.stream(IsolatedMarginCheckResult.class.getRecordComponents())
                .map(RecordComponent::getType)
                .toList();

        assertEquals(
                List.of(
                        FuturesMarginCheckStatus.class,
                        FuturesMarginCheckReason.class,
                        java.util.Optional.class,
                        java.util.Optional.class
                ),
                componentTypes
        );
    }

    /**
     * 確認 gate 不會接受或使用 AccountBalanceView 之類的外部查詢介面。
     */
    @Test
    void requestModelContainsNoExternalBalanceLookupOrCrossMarginField() {
        List<String> components = Arrays.stream(IsolatedMarginCheckRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(
                List.of(
                        "futuresAccount",
                        "leverageConfig",
                        "marketSymbol",
                        "quantity",
                        "entryPrice",
                        "availableMarginAsset",
                        "availableMargin"
                ),
                components
        );
        assertTrue(!components.contains("accountBalanceView"));
        assertTrue(!components.contains("reservation"));
        assertTrue(!components.contains("crossMargin"));
    }

    /**
     * 確認 request 本身是 invariant boundary，gate 不接受 null。
     */
    @Test
    void gateRejectsNullRequest() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> gate.check(null));
        assertEquals("request must not be null", exception.getMessage());
    }

    private static IsolatedMarginCheckRequest request(
            String quantity,
            String entryPrice,
            String availableMargin,
            int leverageMultiplier
    ) {
        return new IsolatedMarginCheckRequest(
                sampleAccount("futures-acct-main", AccountStatus.ACTIVE, "USDT"),
                sampleLeverageConfig("futures-acct-main", "BTC-USDT", leverageMultiplier),
                new FuturesMarketSymbol("BTC-USDT"),
                new FuturesPositionQuantity(new BigDecimal(quantity)),
                new FuturesEntryPrice(new BigDecimal(entryPrice)),
                new AssetSymbol("USDT"),
                amount(availableMargin)
        );
    }

    private static FuturesAccount sampleAccount(String accountId, AccountStatus status, String settlementAsset) {
        return new FuturesAccount(
                new AccountId(accountId),
                new UserId("user-" + accountId),
                status,
                FuturesMarginMode.ISOLATED,
                new AssetSymbol(settlementAsset),
                Instant.parse("2026-07-15T03:04:05Z"),
                Instant.parse("2026-07-15T03:04:06Z")
        );
    }

    private static IsolatedLeverageConfig sampleLeverageConfig(String accountId, String marketSymbol, int leverageMultiplier) {
        return IsolatedLeverageConfig.configure(
                new AccountId(accountId),
                new FuturesMarketSymbol(marketSymbol),
                FuturesLeverage.of(leverageMultiplier),
                Instant.parse("2026-07-15T03:04:07Z")
        );
    }

    private static MoneyAmount amount(String value) {
        return new MoneyAmount(new BigDecimal(value));
    }
}
