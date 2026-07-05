package com.lumix.account;

import com.lumix.common.MoneyAmount;

/**
 * 帳戶餘額唯讀視圖。
 * 這裡只允許查詢，不提供任何直接修改 total / available / locked 的方法。
 */
public interface AccountBalanceView {

    UserId userId();

    AccountId accountId();

    AccountType accountType();

    AssetSymbol asset();

    MoneyAmount total();

    MoneyAmount available();

    MoneyAmount locked();

    AccountStatus status();
}
