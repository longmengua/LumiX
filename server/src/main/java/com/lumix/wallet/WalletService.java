package com.lumix.wallet;

import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;

import java.util.List;

/**
 * 錢包服務邊界。
 * 只定義地址與鏈配置查詢契約，不提供真實鏈上功能。
 */
public interface WalletService {

    // TODO(HUMAN_REVIEW_REQUIRED): 只保留地址供應與查詢契約，不在此層實作真實鏈上動作。
    WalletAddress getOrCreateDepositAddress(UserId userId, AssetSymbol asset, ChainType chain);

    // 查詢使用者已建立的錢包地址。
    List<WalletAddress> listWalletAddresses(UserId userId, AssetSymbol asset);

    // 查詢支援的資產清單。
    List<WalletAsset> listSupportedAssets();

    // 查詢鏈設定，供地址與提現流程使用。
    List<WalletChainConfig> listChainConfigs(AssetSymbol asset);
}
