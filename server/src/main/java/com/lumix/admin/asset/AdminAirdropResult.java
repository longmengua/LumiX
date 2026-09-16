package com.lumix.admin.asset;

/** 空投成功後僅回傳可稽核的 journal 識別碼與是否為安全重送。 */
public record AdminAirdropResult(long ledgerJournalId, boolean replayed) { }
