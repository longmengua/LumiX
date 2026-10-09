package com.lumix.admin.asset;

import java.math.BigDecimal;

/**
 * HTTP 以外的 immutable command。BUSINESS_REVERSAL 的 user/account/asset/direction 不可由此輸入信任。
 */
public record AssetAdjustmentCommand(
        AssetAdjustmentType adjustmentType, String userId, String accountType, String assetSymbol,
        AssetAdjustmentDirection direction, BigDecimal amount, Long sourceLedgerEntryId, String reason,
        String idempotencyKey) { }
