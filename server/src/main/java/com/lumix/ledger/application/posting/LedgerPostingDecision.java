package com.lumix.ledger.application.posting;

/**
 * ledger posting command boundary 的判定結果。
 *
 * 只區分 accepted / rejected，不代表已寫入或已完成任何資金異動。
 */
public enum LedgerPostingDecision {
    ACCEPTED,
    REJECTED
}
