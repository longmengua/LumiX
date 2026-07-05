package com.lumix.ledger;

import com.lumix.account.AccountType;

/**
 * 帳本側帳戶類型。
 * 與 account.AccountType 對應，但刻意保留獨立邊界，避免不同模組直接混用。
 */
public enum LedgerAccountType {
    SPOT,
    FUTURES,
    MARGIN;

    public static LedgerAccountType fromAccountType(AccountType accountType) {
        // 只做明確映射，不接受隱性 fallback，避免新增類型時被默默吞掉。
        return switch (accountType) {
            case SPOT -> SPOT;
            case FUTURES -> FUTURES;
            case MARGIN -> MARGIN;
        };
    }
}
