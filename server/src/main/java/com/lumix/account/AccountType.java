package com.lumix.account;

/**
 * 帳戶類型。
 * 產品帳戶只保留 SPOT 與 FUTURES；現貨槓桿借貸不在 LumiX 產品範圍內。
 */
public enum AccountType {
    SPOT,
    FUTURES
}
