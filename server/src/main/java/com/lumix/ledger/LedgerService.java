package com.lumix.ledger;

/**
 * 帳本服務介面。
 * 只定義行為契約，不提供真實餘額修改實作。
 */
public interface LedgerService {

    // TODO(HUMAN_REVIEW_REQUIRED): 直接整批過帳的入口；正式實作前必須先定義 journal、entry 與交易邊界。
    LedgerJournalResult postJournal(LedgerJournalRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 預留凍結 / 預占用語意，方便未來接 spot / margin / futures 流程。
    LedgerJournalResult reserve(LedgerPostingRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 預留釋放凍結資產語意。
    LedgerJournalResult release(LedgerPostingRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 預留從凍結轉成正式扣帳或入帳的語意。
    LedgerJournalResult commit(LedgerPostingRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 預留回滾語意，用於失敗補償或人工修復流程。
    LedgerJournalResult rollback(LedgerPostingRequest request);
}
