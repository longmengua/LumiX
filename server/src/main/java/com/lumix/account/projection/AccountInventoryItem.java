package com.lumix.account.projection;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import java.time.Instant;
import java.util.Objects;

/** 使用者自己的帳戶容器唯讀資料；它不含餘額，不能被誤當為 balance projection。 */
public record AccountInventoryItem(AccountId accountId, AccountType accountType, AccountStatus accountStatus, Instant createdAt) {
    public AccountInventoryItem { Objects.requireNonNull(accountId, "accountId"); Objects.requireNonNull(accountType, "accountType"); Objects.requireNonNull(accountStatus, "accountStatus"); Objects.requireNonNull(createdAt, "createdAt"); }
}
