package com.lumix.withdrawal.request.eligibility;

/** 風控供給的唯讀結果；UNKNOWN 不得取得 hold handoff。 */
public enum WithdrawalRiskDecision { ALLOWED, REJECTED, UNKNOWN }
