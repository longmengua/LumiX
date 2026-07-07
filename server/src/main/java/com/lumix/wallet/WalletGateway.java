package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

/**
 * 錢包閘道契約。
 * 只保留未來接鏈、掃鏈或地址供應商的介面，不在 Phase 10 實作真實接線。
 */
public interface WalletGateway {

    // TODO(HUMAN_REVIEW_REQUIRED): 供應鏈上地址，實作時需經過 custody 與風控邊界。
    WalletAddress provisionAddress(UserId userId, AssetSymbol asset, ChainType chain);

    // TODO(HUMAN_REVIEW_REQUIRED): 提現廣播入口，正式版本必須先經過提現審核與簽章流程。
    String submitWithdrawal(WithdrawRecord withdrawRecord);

    // 查詢充值交易狀態，供入金確認與對帳使用。
    DepositStatus queryDepositStatus(String txHash, AssetSymbol asset, ChainType chain);
}
