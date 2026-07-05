package com.lumix.ledger;

/**
 * 帳本服務介面。
 * 只定義行為契約，不提供真實餘額修改實作。
 */
public interface LedgerService {

    // 直接整批過帳的入口，後續可對接 journal / event / DB 寫入。
    // TODO: requires high-reasoning review before production use
    LedgerJournalResult postJournal(LedgerJournalRequest request);

    // 預留凍結 / 預占用語意，方便未來接 spot / margin / futures 流程。
    // TODO: requires high-reasoning review before production use
    LedgerJournalResult reserve(LedgerPostingRequest request);

    // 預留釋放凍結資產語意。
    // TODO: requires high-reasoning review before production use
    LedgerJournalResult release(LedgerPostingRequest request);

    // 預留從凍結轉成正式扣帳或入帳的語意。
    // TODO: requires high-reasoning review before production use
    LedgerJournalResult commit(LedgerPostingRequest request);

    // 預留回滾語意，用於失敗補償或人工修復流程。
    // TODO: requires high-reasoning review before production use
    LedgerJournalResult rollback(LedgerPostingRequest request);
}
