package com.lumix.account;

import java.util.List;
import java.util.Optional;

/**
 * 帳戶查詢介面。
 * 只提供讀取，不包含任何餘額變更能力。
 */
public interface AccountQueryService {

    List<AssetAccountView> listAccounts(UserId userId);

    Optional<AssetAccountView> findAssetAccount(UserId userId, AccountType accountType, AssetSymbol asset);
}
