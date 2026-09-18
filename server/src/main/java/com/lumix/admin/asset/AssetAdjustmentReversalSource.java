package com.lumix.admin.asset;

/** 管理端可安全呈現的 authoritative reversal source；remaining 一律由 server 計算。 */
public record AssetAdjustmentReversalSource(String ledgerEntryId, String journalId, String userId, String userEmail,
        String accountType, String assetSymbol, String originalDirection, String originalAmount, String alreadyReversed,
        String remainingReversible, String sourceBusinessType, String sourceBusinessId, boolean eligible, String ineligibleReason) { }
