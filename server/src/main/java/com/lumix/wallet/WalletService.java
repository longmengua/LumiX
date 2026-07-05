package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

import java.util.List;

/**
 * 錢包服務邊界。
 * 只定義地址與鏈配置查詢契約，不提供真實鏈上功能。
 */
public interface WalletService {

    // TODO: requires high-reasoning review before production use
    WalletAddress getOrCreateDepositAddress(UserId userId, AssetSymbol asset, ChainType chain);

    // TODO: requires high-reasoning review before production use
    List<WalletAddress> listWalletAddresses(UserId userId, AssetSymbol asset);

    // TODO: requires high-reasoning review before production use
    List<WalletAsset> listSupportedAssets();

    // TODO: requires high-reasoning review before production use
    List<WalletChainConfig> listChainConfigs(AssetSymbol asset);
}
