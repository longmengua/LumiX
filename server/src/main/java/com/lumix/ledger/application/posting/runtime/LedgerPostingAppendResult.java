package com.lumix.ledger.application.posting.runtime;

import com.lumix.ledger.application.posting.LedgerPostingCommandResult;

import java.util.Objects;

/**
 * 受控 ledger append 的回傳結果。
 *
 * 這個結果只表示 journal / entry append 是否完成，不代表 balance projection、settlement、
 * idempotency、outbox 或 audit 已經完成。
 */
public record LedgerPostingAppendResult(
        LedgerPostingCommandResult commandResult,
        Long ledgerJournalId
) {

    public LedgerPostingAppendResult {
        // 這份結果只服務受控接線，不能把 append 完成誤包成正式 money movement ready。
        Objects.requireNonNull(commandResult, "commandResult must not be null");
        if (commandResult.decision() == com.lumix.ledger.application.posting.LedgerPostingDecision.ACCEPTED) {
            Objects.requireNonNull(ledgerJournalId, "ledgerJournalId must not be null when accepted");
        } else if (ledgerJournalId != null) {
            throw new IllegalArgumentException("ledgerJournalId must be null when rejected");
        }
    }

    /**
     * 判斷受控接線是否真的完成 append。
     *
     * 這裡只看 journal header 是否成功寫入，不延伸解讀成其他交易子系統已同步。
     */
    public boolean appendCompleted() {
        return ledgerJournalId != null;
    }

    /**
     * 建立 append 完成的結果。
     *
     * 這只代表 ledger_journals / ledger_entries 已 append，不能解讀成更多 runtime 已完成。
     */
    public static LedgerPostingAppendResult appended(LedgerPostingCommandResult commandResult, long ledgerJournalId) {
        return new LedgerPostingAppendResult(commandResult, ledgerJournalId);
    }

    /**
     * 建立 rejected 的結果。
     *
     * 這只表示受控接線在前置 gate 就停止，不會 append 任何 ledger row。
     */
    public static LedgerPostingAppendResult rejected(LedgerPostingCommandResult commandResult) {
        return new LedgerPostingAppendResult(commandResult, null);
    }
}
