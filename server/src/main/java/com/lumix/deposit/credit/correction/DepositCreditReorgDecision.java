package com.lumix.deposit.credit.correction;

/**
 * reorg 後唯一允許的後續方向；任何自動覆寫或刪除都不在此模型內。
 */
public enum DepositCreditReorgDecision {
    FREEZE_PENDING_CREDIT,
    APPEND_ONLY_REVERSAL_REQUIRED,
    ESCALATE_HUMAN
}
