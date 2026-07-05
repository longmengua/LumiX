package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

/**
 * 錢包閘道契約。
 * 只保留未來接鏈、掃鏈或地址供應商的介面，不在 Phase 10 實作真實接線。
 */
public interface WalletGateway {

    // TODO: requires high-reasoning review before production use
    WalletAddress provisionAddress(UserId userId, AssetSymbol asset, ChainType chain);

    // TODO: requires high-reasoning review before production use
    String submitWithdrawal(WithdrawRecord withdrawRecord);

    // TODO: requires high-reasoning review before production use
    DepositStatus queryDepositStatus(String txHash, AssetSymbol asset, ChainType chain);
}
