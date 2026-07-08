package com.lumix.ledger.application.posting;

import com.lumix.ledger.application.LedgerRuntimePrerequisite;

import java.util.Set;

/**
 * ledger posting application command boundary。
 *
 * 這個 boundary 只負責 prerequisite 與 invariant gate，然後產生 plan；
 * 它不負責真正寫入任何 ledger row。
 */
public interface LedgerPostingCommandBoundary {

    /**
     * 驗證 command 並回傳 accepted plan 或 rejected result。
     *
     * 檢查順序必須先 prerequisite gate，再 invariant policy，避免把不成立的 runtime 條件誤當成 journal 問題。
     */
    LedgerPostingCommandResult evaluate(LedgerPostingCommand command, Set<LedgerRuntimePrerequisite> availablePrerequisites);
}
