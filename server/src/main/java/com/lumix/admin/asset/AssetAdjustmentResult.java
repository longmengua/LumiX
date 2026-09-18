package com.lumix.admin.asset;

/** 成功 command 的 durable aggregate 與 journal identity。 */
public record AssetAdjustmentResult(long adjustmentId, long ledgerJournalId, boolean replayed) { }
