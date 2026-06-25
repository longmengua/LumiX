/*
 * File purpose: Stable HTTP response for account balances without exposing the mutable domain object.
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AccountResponse(
        long uid,
        BigDecimal balance,
        BigDecimal available,
        BigDecimal frozen,
        BigDecimal orderHold,
        BigDecimal positionMargin,
        Instant updatedAt,
        List<AssetBalanceItem> assets
) {

    /** Maps the internal account aggregate to frontend-friendly balance fields. */
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.uid(),
                account.crossBalance(),
                account.crossAvailable(),
                account.crossHold(),
                account.crossOrderHold(),
                account.crossPositionMargin(),
                account.updatedAt(),
                assetItems(account)
        );
    }

    private static List<AssetBalanceItem> assetItems(Account account) {
        return account.assetBalances().entrySet().stream()
                .map(AccountResponse::toAssetItem)
                .toList();
    }

    private static AssetBalanceItem toAssetItem(Map.Entry<String, Account.AssetBalance> entry) {
        Account.AssetBalance balance = entry.getValue();
        return new AssetBalanceItem(
                entry.getKey(),
                balance.getBalance(),
                balance.getAvailable(),
                balance.getOrderHold(),
                balance.getPositionMargin()
        );
    }

    public record AssetBalanceItem(
            String asset,
            BigDecimal balance,
            BigDecimal available,
            BigDecimal orderHold,
            BigDecimal positionMargin
    ) {
    }
}
