package com.lumix.account;

/**
 * 帳戶劃轉介面。
 * 真正的資產異動必須經由 ledger 邊界處理，這裡只定義 contract。
 */
public interface AccountTransferService {

    AccountTransferResult transfer(AccountTransferRequest request);
}
