package com.lumix.trading.core.futures.account;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Futures sandbox account 的 immutable domain model。
 *
 * 這個 model 只表達最小必要的 futures account 身分與生命週期資訊，方便 Phase 17 逐步補上
 * position / leverage / margin check，但不把任何 runtime money movement 接進來。
 *
 * canonical constructor 是這個型別的唯一 invariant boundary；`open(...)` 只是建立新 ACTIVE
 * isolated account 的 convenience factory，不是唯一建構路徑。
 */
public record FuturesAccount(
        AccountId accountId,
        UserId ownerUserId,
        AccountStatus status,
        FuturesMarginMode marginMode,
        AssetSymbol settlementAsset,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * 建立新的 ACTIVE isolated futures account。
     *
     * 這只是方便呼叫端快速建立標準初始狀態的 convenience factory；真正的不變式仍由
     * canonical constructor 負責，因此 rehydration 或直接建構都會受到同一組驗證保護。
     */
    public static FuturesAccount open(
            AccountId accountId,
            UserId ownerUserId,
            AssetSymbol settlementAsset,
            Instant createdAt
    ) {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        return new FuturesAccount(
                accountId,
                ownerUserId,
                AccountStatus.ACTIVE,
                FuturesMarginMode.ISOLATED,
                settlementAsset,
                createdAt,
                createdAt
        );
    }

    public FuturesAccount {
        // futures account 只是一個 immutable model，但它會被後續 position / margin / risk 流程反覆讀取，
        // 所以在建立時就要把核心欄位與生命週期邊界鎖住，避免半成品進到後續 phase。
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(marginMode, "marginMode must not be null");
        Objects.requireNonNull(settlementAsset, "settlementAsset must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (!marginMode.isIsolated()) {
            throw new IllegalArgumentException("marginMode must be ISOLATED");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }
}
