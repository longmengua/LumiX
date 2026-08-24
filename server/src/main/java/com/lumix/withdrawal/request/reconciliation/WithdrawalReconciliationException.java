package com.lumix.withdrawal.request.reconciliation;
/** request/hold/audit evidence 任一缺失時的 fail-closed exception。 */
public enum WithdrawalReconciliationException { MISSING_HOLD_EVIDENCE, INVALID_AUDIT_TRAIL, NOT_READY_FOR_SIGNER }
