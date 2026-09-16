package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.time.Instant;
import java.util.Objects;

/** 管理端支援畫面可讀取的使用者餘額 projection；它不是帳本真相，也不帶任何寫入能力。 */
public record AdminUserAssetProjection(AccountType accountType, AssetSymbol assetSymbol, MoneyAmount total,
                                       MoneyAmount available, MoneyAmount locked, long projectionVersion,
                                       Instant projectedAt, Instant reconciledAt) {
    public AdminUserAssetProjection {
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(available, "available must not be null");
        Objects.requireNonNull(locked, "locked must not be null");
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
        if (total.isNegative() || available.isNegative() || locked.isNegative() || total.value().compareTo(available.value().add(locked.value())) != 0) {
            throw new IllegalArgumentException("projection amounts must be non-negative and consistent");
        }
    }
}
