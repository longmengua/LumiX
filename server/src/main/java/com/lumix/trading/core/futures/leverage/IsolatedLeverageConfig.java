package com.lumix.trading.core.futures.leverage;

import com.lumix.account.AccountId;
import com.lumix.trading.core.futures.position.FuturesMarketSymbol;

import java.time.Instant;
import java.util.Objects;

/**
 * Isolated margin leverage config 的 immutable sandbox model。
 *
 * 這個 model 只描述單一 futures account 與單一 market 的 leverage 選擇，不表達 cross margin pool、
 * margin calculation、PnL、liquidation 或任何 runtime side effect。
 */
public record IsolatedLeverageConfig(
        AccountId futuresAccountId,
        FuturesMarketSymbol marketSymbol,
        FuturesLeverage leverage,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * 建立新的 isolated leverage config snapshot。
     *
     * 這只是 convenience factory；canonical constructor 仍然是 invariant boundary。
     */
    public static IsolatedLeverageConfig configure(
            AccountId futuresAccountId,
            FuturesMarketSymbol marketSymbol,
            FuturesLeverage leverage,
            Instant configuredAt
    ) {
        Objects.requireNonNull(configuredAt, "configuredAt must not be null");
        return new IsolatedLeverageConfig(
                futuresAccountId,
                marketSymbol,
                leverage,
                configuredAt,
                configuredAt
        );
    }

    /**
     * 以 immutable snapshot 方式調整 leverage。
     *
     * 相同 leverage 也會建立新的 snapshot，因為這代表一次可審計的 reconfigure 事件，
     * 而不是原物件原地修改。
     */
    public IsolatedLeverageConfig reconfigure(FuturesLeverage newLeverage, Instant changedAt) {
        Objects.requireNonNull(newLeverage, "newLeverage must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException("changedAt must not be before updatedAt");
        }
        return new IsolatedLeverageConfig(
                futuresAccountId,
                marketSymbol,
                newLeverage,
                createdAt,
                changedAt
        );
    }

    public IsolatedLeverageConfig {
        // Leverage config 必須同時鎖定 account 與 market，這樣才不會把 isolated config 寫成共享風險池。
        Objects.requireNonNull(futuresAccountId, "futuresAccountId must not be null");
        Objects.requireNonNull(marketSymbol, "marketSymbol must not be null");
        Objects.requireNonNull(leverage, "leverage must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }
}
