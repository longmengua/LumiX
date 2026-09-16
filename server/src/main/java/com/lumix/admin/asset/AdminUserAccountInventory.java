package com.lumix.admin.asset;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import java.time.Instant;
import java.util.Objects;

/**
 * 管理端可檢視的使用者帳戶容器。它刻意不攜帶金額，避免將「已開戶」誤讀成已有資產餘額。
 */
public record AdminUserAccountInventory(AccountId accountId, AccountType accountType, AccountStatus accountStatus,
                                        Instant createdAt) {
    public AdminUserAccountInventory {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(accountStatus, "accountStatus must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
