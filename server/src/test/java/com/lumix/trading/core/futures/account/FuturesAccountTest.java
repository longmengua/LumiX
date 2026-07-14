package com.lumix.trading.core.futures.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 驗證 futures account sandbox model 只保留 Phase 17-T01 需要的最小不變式。
 */
class FuturesAccountTest {

    /**
     * 確認 convenience factory 會建立標準的 ACTIVE + ISOLATED futures account。
     *
     * 這個 case 必須存在，因為 open(...) 只是快速建立標準初始狀態的 helper，
     * 不是取代 canonical constructor 的唯一入口。
     */
    @Test
    void openCreatesConvenienceActiveIsolatedFuturesAccount() {
        Instant createdAt = Instant.parse("2026-07-14T01:02:03Z");

        FuturesAccount account = FuturesAccount.open(
                new AccountId("futures-acct-001"),
                new UserId("user-001"),
                new AssetSymbol("usdt"),
                createdAt
        );

        assertEquals(new AccountId("futures-acct-001"), account.accountId());
        assertEquals(new UserId("user-001"), account.ownerUserId());
        assertEquals(AccountStatus.ACTIVE, account.status());
        assertEquals(FuturesMarginMode.ISOLATED, account.marginMode());
        assertEquals(new AssetSymbol("USDT"), account.settlementAsset());
        assertEquals(createdAt, account.createdAt());
        assertEquals(createdAt, account.updatedAt());
    }

    /**
     * 確認 canonical constructor 可以直接建立有效 futures account。
     *
     * 這個 case 必須存在，因為 canonical constructor 是 rehydration 與直接建構的共同驗證邊界，
     * 不能把所有合法建構都誤寫成只剩 convenience factory。
     */
    @Test
    void canonicalConstructorAllowsDirectInstantiation() {
        Instant createdAt = Instant.parse("2026-07-14T02:03:04Z");
        Instant updatedAt = Instant.parse("2026-07-14T02:03:05Z");

        FuturesAccount account = new FuturesAccount(
                new AccountId("futures-acct-004"),
                new UserId("user-004"),
                AccountStatus.CLOSED,
                FuturesMarginMode.ISOLATED,
                new AssetSymbol("USDT"),
                createdAt,
                updatedAt
        );

        assertEquals(new AccountId("futures-acct-004"), account.accountId());
        assertEquals(new UserId("user-004"), account.ownerUserId());
        assertEquals(AccountStatus.CLOSED, account.status());
        assertEquals(FuturesMarginMode.ISOLATED, account.marginMode());
        assertEquals(new AssetSymbol("USDT"), account.settlementAsset());
        assertEquals(createdAt, account.createdAt());
        assertEquals(updatedAt, account.updatedAt());
    }

    /**
     * 確認 margin mode 不會偷渡成 cross margin 或其他未核准模式。
     *
     * 這個 case 必須存在，因為 Phase 17 目前只允許 isolated margin，
     * 一旦 enum 變成多值，這個 guard 可以提早攔掉未完成的 risk model。
     */
    @Test
    void marginModeSupportsOnlyIsolated() {
        assertEquals(List.of(FuturesMarginMode.ISOLATED), List.of(FuturesMarginMode.values()));
        assertTrue(FuturesMarginMode.ISOLATED.isIsolated());
    }

    /**
     * 確認模型不接受時間倒序，避免生命週期資料在後續查詢或重建時失真。
     *
     * 這個 case 必須存在，因為 futures account 會被 position 與 margin 規則反覆讀取，
     * 若 createdAt / updatedAt 不一致，後續審計與 replay 會出現無法解釋的資料。
     */
    @Test
    void constructorRejectsTimestampRegression() {
        Instant createdAt = Instant.parse("2026-07-14T01:02:03Z");
        Instant updatedAt = Instant.parse("2026-07-14T01:02:02Z");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FuturesAccount(
                        new AccountId("futures-acct-002"),
                        new UserId("user-002"),
                        AccountStatus.ACTIVE,
                        FuturesMarginMode.ISOLATED,
                        new AssetSymbol("USDT"),
                        createdAt,
                        updatedAt
                )
        );

        assertEquals("updatedAt must not be before createdAt", exception.getMessage());
    }

    /**
     * 確認必要欄位不允許空值，避免半成品 futures account 進到後續 phase。
     *
     * 這個 case 必須存在，因為 account identity、owner identity 與 settlement asset 都是
     * futures core model 的必要前置條件，缺一個就不能被視為可用資料。
     */
    @Test
    void constructorRejectsNullMandatoryFields() {
        Instant createdAt = Instant.parse("2026-07-14T01:02:03Z");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new FuturesAccount(
                        null,
                        new UserId("user-003"),
                        AccountStatus.ACTIVE,
                        FuturesMarginMode.ISOLATED,
                        new AssetSymbol("USDT"),
                        createdAt,
                        createdAt
                )
        );

        assertEquals("accountId must not be null", exception.getMessage());
    }
}
