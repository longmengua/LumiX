package com.lumix.withdrawal.broadcast;

/** broadcast evidence 不完整或不一致時的 fail-closed 類別；沒有任何修復或資金操作語意。 */
public enum WithdrawalBroadcastReconciliationException { INTENT_DIGEST_MISMATCH, DUPLICATE_ATTEMPT_ID, TERMINAL_STATE_REGRESSION, CONFIRMATION_NOT_FINAL }
