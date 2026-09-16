package com.lumix.admin.asset;

import java.time.Instant;

/** 現貨幣種設定的管理端投影；internalName 為交易所內部名稱，不含錢包或鏈上資料。 */
public record AdminSpotAssetConfiguration(
        String assetSymbol,
        String internalName,
        int precisionScale,
        String status,
        Instant updatedAt
) { }
